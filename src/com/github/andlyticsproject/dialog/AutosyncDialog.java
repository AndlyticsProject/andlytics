package com.github.andlyticsproject.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import com.github.andlyticsproject.R;
import com.github.andlyticsproject.sync.AutosyncHandler;
import com.github.andlyticsproject.sync.AutosyncHandlerFactory;

public class AutosyncDialog extends Dialog implements OnClickListener {

	public static final int TAG_IMAGE_REF = R.id.tag_mainlist_image_reference;

	Button okButton;

	private LayoutInflater layoutInflater;

	private Activity context;

	private String[] periodTexts;

	private Integer[] periodValues = {0, 60 * 15, 60 * 30, 60 * 60, 60 * 60 * 2, 60 * 60 * 3, 60 * 60 * 6, 60 * 60 * 8, 60 * 60 * 12, 60 * 60 * 24};

    private TextView periodTextView;

    private View periodPlusButton;

    private View periodMinusButton;

    private TextView periodWarning;

	public interface GhostSelectonChangeListener {

		public void onGhostSelectionChanged(String packageName, boolean isGhost);

		public void onGhostDialogClose();

	}

    public AutosyncDialog(final Activity context, final String accountName) {

        super(context, R.style.Dialog);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        layoutInflater = context.getLayoutInflater();

        setContentView(R.layout.autosync_dialog);

        periodTexts = context.getResources().getStringArray(R.array.autosync_periods);

        View closeButton = (View) this.findViewById(R.id.notification_dialog_close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
        ListView lv = (ListView) this.findViewById(R.id.list_view_id);
        lv.addHeaderView(layoutInflater.inflate(R.layout.autosync_list_header, null), null, false);
        lv.setAdapter(new BaseAdapter() {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return null;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public int getCount() {
                return 0;
            }
        });

		this.context = context;

		periodTextView = (TextView) this.findViewById(R.id.notification_dialog_period_text);
        periodPlusButton = (View) this.findViewById(R.id.notification_dialog_period_plus);
        periodMinusButton = (View) this.findViewById(R.id.notification_dialog_period_minus);
        periodWarning = (TextView) this.findViewById(R.id.notification_dialog_warning);


		updatePeriod(accountName, 0);

		periodPlusButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                updatePeriod(accountName, 1);
            }
        });

        periodMinusButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                updatePeriod(accountName, -1);
            }
        });

    }


    private void updatePeriod(String accountName, int increase) {

        AutosyncHandler autosyncHandler = AutosyncHandlerFactory.getInstance(context);
        int autosyncPeriod = autosyncHandler.getAutosyncPeriod(accountName);

		List<Integer> periodList = Arrays.asList(periodValues);
		int periodIndex = periodList.indexOf(autosyncPeriod);
		if(periodIndex < 0) {
		    periodIndex = 0;
		}

		if(increase < 0) {
		    periodIndex--;
		    if(periodIndex < 0) {
		        periodIndex = periodList.size() -1;
		    }
		} else if (increase > 0) {
		    periodIndex++;
		    if(periodIndex >= periodList.size()) {
		        periodIndex = 0;
		    }
		}

		if(autosyncPeriod != periodList.get(periodIndex)) {
		    autosyncHandler.setAutosyncPeriod(accountName, periodList.get(periodIndex));
		}

		String periodString = periodTexts[periodIndex];
		periodTextView.setText(periodString);

		if(periodIndex == 0) {
		    periodWarning.setBackgroundColor(Color.parseColor("#ff0000"));
		    periodWarning.setTextColor(Color.parseColor("#ffffff"));
		} else {
            periodWarning.setBackgroundColor(Color.parseColor("#ffffff"));
            periodWarning.setTextColor(Color.parseColor("#ffffff"));
		}

    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void onClick(View v) {
        /** When OK Button is clicked, dismiss the dialog */
        if (v == okButton)
            dismiss();
    }


}
