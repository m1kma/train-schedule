package mjm.trainschedule;

import java.util.HashMap;
import java.util.List;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
 
/**
 * Used to populate train details list. Colorize passed stations by different color.
 * @author Mäkelä
 *
 */
public class ColoredAdapterTrain extends SimpleAdapter {
    private int[] colors = new int[] { 0x00FFFFFF, 0xFFBBBBBB, 0xFFFFFFFF, 0xFF000000};
     
    public ColoredAdapterTrain(Context context, List<HashMap<String, String>> items, int resource, String[] from, int[] to) {
        super(context, items, resource, from, to);
    }
 
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      
		View view = super.getView(position, convertView, parent);
		TextView completedView = (TextView) ((ViewGroup) view).getChildAt(0);
		TextView timeView = (TextView) ((ViewGroup) view).getChildAt(1);
		TextView titleView = (TextView) ((ViewGroup) view).getChildAt(2);

		String completedChar = completedView.getText().toString();

		if (completedChar.equals("1")) { // train has pass station --> use different UI color
			view.setBackgroundColor(colors[1]);
			timeView.setTextColor(colors[2]);
			titleView.setTextColor(colors[2]);
		}
		else
		{
			view.setBackgroundColor(colors[0]);
			timeView.setTextColor(colors[3]);
			titleView.setTextColor(colors[3]);
		} 

		return view;
    }
}