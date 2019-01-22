package mjm.trainschedule;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;

/**
 * <p>Purpose of application is to give quick way to see schedule of trains for selected station.
 * Application coverts commuter traffic trains in Helsinki region.
 * Schedule will show trains approximately within two hours from current moment. Timeframe is limited by RSS feed.
 * Application uses free of use RSS feed that is provided by VR. UI is localized to english and finnish.
 * 
 * <p>NOTE: If application is run in emulator, remember to check that system time and date is correctly 
 * setted in emulator (time is used to query schedule data).
 * 
 * <p>Application contains following main elements: 
 * <ul>
 * <li> MainActivity contains logic to show trains for selected station. Activity contains spinner for station selection 
 *      and list of trains. List items are colored by train ID. There is also logic to automatically refresh
 *      list within selected time interval.
 * <li> TrainDetailsActivity contains logic to show stations where selected train is stopping. Activity takes as an
 *      argument station code from MainActivity and executes search to RSS-feed. List items (stations) contains
 *      onClick event to allow user to navigate back to main page and show details of selected stations. 
 * <li> Settings page is implemented as an Fragment that is placed inside of empty activity SettingsFrag.
 * <li> Two custom ListAdapters are used to have list items colorize. ColoredAdapter, ColoredAdapterTrain
 * <li> XMLParser is used to parse RSS feed XML and store data to object model.
 * </ul>
 * <p>
 * 
 * Features:
 * <ul>
 * <li> Select station from dropdown list
 * <li> Show color coded schedule by two directions: trains going to Helsinki and trains coming from Helsinki
 * <li> Click train item from list to show train details and stop stations
 * <li> From options menu choose favorite stations those are added to top of the dropdown list
 * <li> From options menu show image of commuter trainlines of Helsinki region 
 * </ul>
 *
 * New:
 * <ul>
 * <li> Improved UI to more professional look. Optimized to Android 5 Phone size devices.
 * <li> Train list colored by train ID (character) to provide better overview. Coloring can be switch On and Off from Settings.
 * <li> All UI texts localized to english and finnish. Based on Android system local.
 * <li> Enhanced train details list to allow user to click station item and jump back to main screen
 * <li> Enhanced train details list  to show passed stations by different color in case when train is
 *      travelling. This gives info where train is going. Data based RSS.
 * <li> Added option to set automatic refresh rate for main train list. Refresh interval can selected from SettingsFrag.
 * <li> Added option to show long-distance trains.
 * <li> Settings page changed to Fragment based (previously activity)
 * </ul>
 * 
 * <p>Application is tested on emulator and Nexus 5 (lollipop) device.
 * 
 * @author Mika Mäkelä
 */
public class MainActivity extends Activity implements OnItemSelectedListener {
	
	private String[] stationsArrLabel;
	private String[] stationsArrCode;
	private ProgressBar progressBar;
	private String callMode;
	private Spinner spinner;
	private ListView lv;
	private Editor editor;
	private SharedPreferences pref;
    Handler handler = new Handler();
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        spinner = (Spinner) findViewById(R.id.stationSpinner);
        progressBar = (ProgressBar) findViewById(R.id.markerProgress);
        lv = (ListView)findViewById(R.id.listview);    
        
    	Resources res = getResources();    	
        stationsArrLabel = res.getStringArray(R.array.stationEntries);
        stationsArrCode = res.getStringArray(R.array.stationValues);
        
        pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        
        runnable.run();
    }

    
    @Override
    protected void onResume() {
    	super.onResume();
    	fillSpinner(); // refresh spinner    	
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
       
    	// TrainDetails activity will return result when station is clicked on list.
    	// --> Then make query by result station title.
        if (requestCode == 1) {
            if(resultCode == RESULT_OK){
            	String stationCode = solveStationCode(data.getStringExtra("STATION_TITLE"));
            	
            	spinner.setSelection(solveStationSpinnerPosition(stationCode));
            	
                callMode = "OTHERS";
                selectStationAndCallSchedule(stationCode);
            }
            if (resultCode == RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    };
    
    /**
     * Method will fill spinner by stations.
     * Put favorite stations to top of the list if user has selected favorites from Settings page.
     */
    private void fillSpinner() {
    	
    	 ////// Read favorite stations from SharedPreferences and put those to top of the list //////
    	//
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> favoriteStationsList = sharedPref.getStringSet("favorite_stations", null);
        List<String> finalStationsList = new ArrayList<String>();
        
        // If favorite stations exists
        if (favoriteStationsList != null && favoriteStationsList.size() > 0) {
	        Object[] favoriteStationsArr = favoriteStationsList.toArray();
	        
	        for (int i = 0; i < favoriteStationsArr.length; i++) {
	        	finalStationsList.add("* " + solveStationName(favoriteStationsArr[i].toString()));
	        }
        }
        
        
        
         ///// Put all other stations to list /////
        //
        for (int i = 0; i < stationsArrLabel.length; i++) {        	
        	finalStationsList.add(solveStationName(stationsArrLabel[i]));
		}
        
        Object[] finalStationsArrO = finalStationsList.toArray();
        String[] finalStationsArr = new String[finalStationsArrO.length];

        for (int i = 0; i < finalStationsArrO.length; i++) {
        	finalStationsArr[i] = finalStationsArrO[i].toString();
		}        

        spinner.setOnItemSelectedListener(this);
        
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, finalStationsArr);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setSelection(pref.getInt("SpinnerItem", 0)); // set previous spinner state

        // ListView Item Click Listener
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				// ListView Clicked item value
				@SuppressWarnings("unchecked")
				HashMap<String, String> itemValue = (HashMap<String, String>) lv.getItemAtPosition(position);

				Intent intent = new Intent(MainActivity.this, TrainDetailsActivity.class);
			    intent.putExtra("TRAIN_GUID", itemValue.get("guid"));
			    intent.putExtra("TRAIN_TITLE", itemValue.get("title"));
			    intent.putExtra("TRAIN_TO", itemValue.get("to"));
			    startActivityForResult(intent, 1);
			}
		});
    }
    
    
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

    }
    
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }
    
    /**
     * Event listener for Helsinki line button
     * @param v
     */
    public void selectStationHelBtn_onClick(View v) {
    	callMode = "HELSINKI";
    	handler.removeCallbacks(runnable);
    	selectStationAndCallSchedule(null);
    }
    
    /**
     * Event listener for Others line button
     * @param v
     */
    public void selectStationOtherBtn_onClick(View v) {
    	callMode = "OTHERS";
    	handler.removeCallbacks(runnable);
    	selectStationAndCallSchedule(null);
    }
    
    
    /**
     * runnable used to periodically refresh train list
     */
    Runnable runnable = new Runnable() {
        public void run() {
        	selectStationAndCallSchedule(null);
        }
    };
    
    
    /**
     * Run the actual query to RSS feed interface.
     * @param selectedStationCode
     */
    private void selectStationAndCallSchedule(String selectedStationCode) {
    	
    	if (spinner.getSelectedItem() != null) {
	    	if (selectedStationCode == null)
	    		selectedStationCode = solveStationCode(spinner.getSelectedItem().toString());
	    	
	    	lv.setAdapter(null);
	    	progressBar.setVisibility(View.VISIBLE);
	
	        editor = pref.edit();
	    	editor.putString("SelectedStation", selectedStationCode); 
	    	editor.putString("CallMode", callMode);
	    	editor.putInt("SpinnerItem", spinner.getSelectedItemPosition());
	    	editor.commit(); // commit changes
	
			new HttpRequestTask().execute(getString(R.string.feed_url) + selectedStationCode);

	        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
	        String refreshRate = sharedPref.getString("refresh_rate", "0");
	        int refreshRateInt = Integer.parseInt(refreshRate);
	        
	        if (refreshRateInt > 0)
	        	handler.postDelayed(runnable, refreshRateInt); // set automatic refresh for list
    	}
    }
        
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
        	Intent i = new Intent(this, SettingsFrag.class);
            startActivityForResult(i, 1);
        	return true;
        }
        
        if (id == R.id.about_dialog) {
        	AboutDialog ad = new AboutDialog();
        	ad.show(getFragmentManager(), "about");
        	return true;
        }
        
        if (id == R.id.routemap_img) {
        	Intent intent = new Intent(this, ImageActivity.class);
            startActivity(intent);
        	return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Solve station user friendly name from tree letter station code.
     * @param stationCode
     * @return
     */
    private String solveStationName(String stationCode) {
    	
    	String stationName = "";
    	
    	for (int i = 0; i < stationsArrCode.length; i++) {
    		if (stationsArrCode[i].equals(stationCode)) {
    			stationName = stationsArrLabel[i];
    		}
		}
    	
    	if (stationName.length() == 0)
    		stationName = stationCode;
    	
    	return stationName;
    }
    
    /**
     * Solve tree letter station code from user friendly station name.
     * @param stationName
     * @return
     */
    private String solveStationCode(String stationName) {
    	
    	stationName = stationName.replace("* ", "");
    	String stationCode = "";
    	
    	for (int i = 0; i < stationsArrLabel.length; i++) {
    		if (stationsArrLabel[i].equals(stationName)) {
    			stationCode = stationsArrCode[i];
    		}
		}
    	
    	if (stationCode.length() == 0)
    		stationCode = stationName;
    	
    	return stationCode;
    }
    
    /**
     * Solve spinner position id by station code.
     * @param stationCode
     * @return
     */
    private int solveStationSpinnerPosition(String stationCode) {
    	
    	int spinnerPosition = 0;
    	
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> favoriteStationsList = sharedPref.getStringSet("favorite_stations", null);
    	
    	for (int i = 0; i < stationsArrCode.length; i++) {
    		if (stationsArrCode[i].equals(stationCode)) {
    			spinnerPosition = i;
    			break;
    		}
		}
    	
    	spinnerPosition = spinnerPosition + favoriteStationsList.size();
    	
    	return spinnerPosition;
    }
    
    
    /**
     * Use Async task to query station RSS feed and parse feed to screen.
     */
	class HttpRequestTask extends AsyncTask<String, Void, List<XMLParser.TrainItem>> {		

	    protected List<XMLParser.TrainItem> doInBackground(String... urls) {

	    	List<XMLParser.TrainItem> trainList = null;
	    	
	        try {		        	
	    		HttpClient httpclient = new DefaultHttpClient();
	            HttpGet httpget = new HttpGet(urls[0]);

                HttpResponse response = httpclient.execute(httpget);
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    InputStream inputstream = entity.getContent();                    
                    XMLParser parser = new XMLParser();                   
                    trainList = parser.parse(inputstream);
                    inputstream.close();
                }	        	
	        } catch (Exception e) {
	            return null;
	        }
	        
	        return trainList;
	    }

	    /**
	     * Executed when http query is ready.
	     */
	    protected void onPostExecute(List<XMLParser.TrainItem> trainList) {
    	
	    	progressBar.setVisibility(View.GONE);

	    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
   	        Boolean showAllTrainsOnList = sharedPref.getBoolean("enable_all_trains", false);
   	        Boolean enableColorList = sharedPref.getBoolean("enable_colored_list", true);    	
	    	
	    	SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd HH:mm");  
   		    SimpleDateFormat format2 = new SimpleDateFormat("yyyyMMdd");
	    	
   		    Date currentDate = new Date();
   		    
   		    // prepare the list of all records
   	        List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
   		    
   		    if (trainList != null) {   		    
		    	for (XMLParser.TrainItem trainItem : trainList) {		
	
		    		// Filter out all other that local trains
		    		if (!trainItem.category.equals("2") && !showAllTrainsOnList)
		    			continue;
		    		
		    		// Filter out trains by button that user has pressed.
		    		// Show trains to Helsinki or trains to other stations.
		    		if (callMode.equals("HELSINKI") && !trainItem.toStation.equals("HKI"))
		    			continue;
		    		else if (callMode.equals("OTHERS") && trainItem.toStation.equals("HKI"))
		    			continue;
		    		
		    		Date trainEta = null;
	
		    		// Create timestamp that corresponds current date and estimated time of train
			    	try {  
			    		trainEta = format.parse(format2.format(currentDate) + " " + trainItem.eta);  
			    	} catch (ParseException e) {  
			    	    e.printStackTrace();  
			    	}
		    		
			    	// Show only trains scheduled later than NOW.
			    	if (trainEta.after(new Date())) {				    		
		   	            HashMap<String, String> map = new HashMap<String, String>();			    		
		   	            
		   	            map.put("guid", trainItem.guid);
		   	            map.put("title", trainItem.title);
		   	            
		   	            // Show estimated time if it differs from scheduled time (=train is late)
			    		if (!trainItem.scheduledTime.equals(trainItem.eta)) {
			    			map.put("time", trainItem.scheduledTime + " \u2192 " + trainItem.eta);
			    		} else {
			    			map.put("time", trainItem.scheduledTime);
			    		}
	
			    		map.put("to", solveStationName(trainItem.toStation));
			    		
		   	            fillMaps.add(map);
			    	}
				}
   		    }
   		    
   	        // Create the grid item mapping
   	        String[] from = new String[] {"title", "time", "to"};
   	        int[] to = new int[] { R.id.listitem1, R.id.listitem2,R.id.listitem4 };
   	        
   	        int gridLayoutId = 0;
   	        
   	        // Choose item layout depending are all trains displayed or not
   	        if (showAllTrainsOnList)
   	        	gridLayoutId = R.layout.grid_item_all_trains;
   	        else
   	        	gridLayoutId = R.layout.grid_item;
   	        
   	        // Fill in the grid_item layout
   	        if (enableColorList) {
	   	        ColoredAdapter adapter = new ColoredAdapter(MainActivity.this, fillMaps, gridLayoutId, from, to);
	   	        lv.setAdapter(adapter);
   	        } else {
   	        	SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, fillMaps, gridLayoutId, from, to);
	   	        lv.setAdapter(adapter);
   	        }		
	    }	
	}
   
}
