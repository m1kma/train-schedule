package mjm.trainschedule;

import java.util.HashMap;
import java.util.List;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
 
/**
 * Colored adapter is used to have lines colored by train ID.
 * @author Mäkelä
 *
 */
public class ColoredAdapter extends SimpleAdapter {
    private int[] colors = new int[] { 0x00FFFFFF, 0x88FF9933, 0x8800CC00, 0x8899CCFF, 0x88FF6666 };
     
    public ColoredAdapter(Context context, List<HashMap<String, String>> items, int resource, String[] from, int[] to) {
        super(context, items, resource, from, to);
    }
 
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      
		View view = super.getView(position, convertView, parent);
		TextView trainCharTV = (TextView) ((ViewGroup) view).getChildAt(0);

		String trainChar = trainCharTV.getText().toString();

		// trains are divided to separate categories based to line
		if (trainChar.equals("M")) {
			view.setBackgroundColor(colors[1]);
		}
		else if (trainChar.equals("Y") || trainChar.equals("S")
				|| trainChar.equals("U") || trainChar.equals("L")
				|| trainChar.equals("E") || trainChar.equals("A")) 
		{
			view.setBackgroundColor(colors[2]);
		} 
		else if (trainChar.equals("I") || trainChar.equals("K")
				|| trainChar.equals("N") || trainChar.equals("T")
				|| trainChar.equals("H") || trainChar.equals("R")) 
		{
			view.setBackgroundColor(colors[3]);
		} 
		else if (trainChar.equals("Z")) 
		{
			view.setBackgroundColor(colors[4]);
		} 
		else 
		{
			view.setBackgroundColor(colors[0]);
		}

		return view;
    }
}