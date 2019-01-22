package mjm.trainschedule;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Activity is used to show train details list. List contains stations where selected train is stopping.
 * Station list is populated using custom list adapter to have color coding. If train is travelling, passed
 * stations are shown in different color.
 * 
 * List contains onClick event. By clicking list item (=station) main activity is loaded by passing
 * selected station item as a parameter. This allows user to jump from train details to main page and back to details.
 * @author Mäkelä
 *
 */
public class TrainDetailsActivity extends Activity {

	private String[] stationsArrLabel;
	private String[] stationsArrCode;
	private ListView lv;
	private ProgressBar progressBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupActionBar();
		setContentView(R.layout.activity_train_details);
		
		lv = (ListView)findViewById(R.id.listDetailsView); 
		progressBar = (ProgressBar) findViewById(R.id.markerDetailsProgress);
		
		Resources res = getResources();    	
        stationsArrLabel = res.getStringArray(R.array.stationEntries);
        stationsArrCode = res.getStringArray(R.array.stationValues);
        
        // Get the message from the intent
        Intent intent = getIntent();
        String trainGuid = intent.getStringExtra("TRAIN_GUID");
        String trainTitle = intent.getStringExtra("TRAIN_TITLE");
        String trainTo = intent.getStringExtra("TRAIN_TO");
        
        TextView trainHead = (TextView) findViewById(R.id.trainHeader);
        
        trainHead.setText(trainTitle + " " + trainTo);
        
        // ListView Item Click Listener --> when station is clicked
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				// ListView Clicked item value
				@SuppressWarnings("unchecked")
				HashMap<String, String> itemValue = (HashMap<String, String>) lv.getItemAtPosition(position);

				Intent intent = new Intent();
			    intent.putExtra("STATION_TITLE", itemValue.get("title"));
			    setResult(RESULT_OK, intent);
			    finish();
			}
		});
        
        new HttpRequestTask().execute(getString(R.string.train_url) + trainGuid);
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Show the Up button in the action bar.
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
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
     * Use Async task to query station RSS feed and parse feed to screen.
     *
     */
	class HttpRequestTask extends AsyncTask<String, Void, List<XMLParser.TrainDetailsItem>> {		

	    protected List<XMLParser.TrainDetailsItem> doInBackground(String... urls) {

	    	progressBar.setVisibility(View.VISIBLE);
	    	List<XMLParser.TrainDetailsItem> trainDetailsList = null;
	    	
	        try {		        	
	    		HttpClient httpclient = new DefaultHttpClient();
	            HttpGet httpget = new HttpGet(urls[0]);
  
                HttpResponse response = httpclient.execute(httpget);
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    InputStream inputstream = entity.getContent();                    
                    XMLParser parser = new XMLParser();                   
                    trainDetailsList = parser.parseDetails(inputstream);
                    inputstream.close();
                }	        	
	        } catch (Exception e) {
	            return null;
	        }
	        
	        return trainDetailsList;
	    }

	    protected void onPostExecute(List<XMLParser.TrainDetailsItem> trainDetailsList) {
    	
	    	progressBar.setVisibility(View.GONE);
	    	SimpleDateFormat format = new SimpleDateFormat("HH:mm");  
   		    
   		    // prepare the list of all records
   	        List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
	            
   		    if (trainDetailsList != null) {   		    
		    	for (XMLParser.TrainDetailsItem trainItem : trainDetailsList) {		
		    		
	   	            HashMap<String, String> map = new HashMap<String, String>();
		    		
	   	            Calendar trainEtaCal = new GregorianCalendar();
	   	         	Calendar trainScheduledCal = new GregorianCalendar();
		    		
	   	            try {
			    		trainEtaCal.setTime(format.parse(trainItem.eta));
			    		trainScheduledCal.setTime(format.parse(trainItem.scheduledTime));
	   	            } catch (ParseException e) {  
			    	    e.printStackTrace();  
			    	}
	   	            
	   	            // Show estimated time if it differs from scheduled time more than one minute (=train is late)
	   	            if (trainScheduledCal != null && trainEtaCal != null) 
	   	            {
	   	            	if ( trainScheduledCal.get(Calendar.MINUTE) == trainEtaCal.get(Calendar.MINUTE)
	   	            			|| trainScheduledCal.get(Calendar.MINUTE) + 1 == trainEtaCal.get(Calendar.MINUTE)
	   	            			|| trainScheduledCal.get(Calendar.MINUTE) - 1 == trainEtaCal.get(Calendar.MINUTE)) 
	   	            	{
	   	            		map.put("time", trainItem.scheduledTime);
			    		} 
	   	            	else 
	   	            	{
	   	            		map.put("time", trainItem.scheduledTime + " \u2192 " + trainItem.eta);
			    		}
	   	            }
	   	            	
	   	            map.put("completed", trainItem.completed);	   	            
	   	            map.put("title", solveStationName(trainItem.title));
		    		
	   	            fillMaps.add(map);
				}
   		    }
   		    
   	        // Create the grid item mapping
   	        String[] from = new String[] {"completed", "time", "title"};
   	        int[] to = new int[] { R.id.listitem1, R.id.listitem2, R.id.listitem3};

   	        // Fill in the grid_item layout
   	        SimpleAdapter adapter = new ColoredAdapterTrain(TrainDetailsActivity.this, fillMaps, R.layout.grid_details_item, from, to);
   	        lv.setAdapter(adapter);
		
	    }
	}
    
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.train_details, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
