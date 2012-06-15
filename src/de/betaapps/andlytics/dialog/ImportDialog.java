package de.betaapps.andlytics.dialog;

import de.betaapps.andlytics.R;
import de.betaapps.andlytics.io.ImportService;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ImportDialog extends Dialog implements OnClickListener {

	public static final int TAG_IMAGE_REF = R.id.tag_mainlist_image_reference;

	Button okButton;
    
	private ImportListAdapter adapter;

	private List<String> files;

	private LayoutInflater layoutInflater;
	
	private List<String> importFileNames = new ArrayList<String>();
	
    public ImportDialog(final Activity context, List<String> fileName, final String accountName) {
        
    	
        super(context, R.style.Dialog);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        layoutInflater = context.getLayoutInflater();
        
        setContentView(R.layout.import_dialog);
        
        adapter = new ImportListAdapter();
        this.files = fileName;
        
        View closeButton = (View) this.findViewById(R.id.import_dialog_close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
        
        
        View importButton = (View) this.findViewById(R.id.import_dialog_import_button);
        importButton.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {

                
                if (!android.os.Environment.getExternalStorageState().equals(
                                android.os.Environment.MEDIA_MOUNTED)) {
                    Toast.makeText(context, "SD-Card not mounted, can't import!", Toast.LENGTH_LONG).show();
                    
                } else {
                    
                    if (importFileNames.size() == 0) {
                        
                        Toast.makeText(context, "No app selected, can't import!", Toast.LENGTH_LONG).show();
                        
                    } else {
                        
                        Intent intent = new Intent(context, ImportService.class);
                        intent.putExtra(ImportService.FILE_NAMES, importFileNames.toArray(new String[importFileNames.size()]));
                        intent.putExtra(ImportService.ACCOUNT_NAME, accountName);
                        context.startService(intent);
                        
                        dismiss();
                    }
                    
                }
                
                
            }
        });
        ListView lv = (ListView) this.findViewById(R.id.list_view_id);
        lv.addHeaderView(layoutInflater.inflate(R.layout.import_list_header, null), null, false);
        lv.setAdapter(adapter);
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
    
    class ImportListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return files.size();
		}

		@Override
		public String getItem(int position) {
			return files.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder holder;

			if (convertView == null) {

				convertView = layoutInflater.inflate(R.layout.import_list_item, null);

				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.import_file_name); 
				holder.row = (RelativeLayout) convertView.findViewById(R.id.import_app_row);
				holder.checkbox = (CheckBox)convertView.findViewById(R.id.import_file_checkbox);
				convertView.setTag(holder);

			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			final String fileName = getItem(position);
			holder.name.setText(fileName);
			
			holder.checkbox.setChecked(importFileNames.contains(fileName));
			
			holder.row.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {

					CheckBox checkbox = ((CheckBox)(((ViewGroup)v).findViewById(R.id.import_file_checkbox)));
					checkbox.setChecked(!checkbox.isChecked());
					
			        if(checkbox.isChecked()) {
                        importFileNames.add(fileName);
                    } else {
                        importFileNames.remove(fileName);
                    }

				}
			});
			
			holder.checkbox.setTag(fileName);

            holder.checkbox.setOnClickListener(new CheckBox.OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    boolean isChecked = ((CheckBox)v).isChecked();
				    
                    
                    if(isChecked) {
                        importFileNames.add(fileName);
                    } else {
                        importFileNames.remove(fileName);
                    }
				    
				}
			});

			return convertView;
		}
		
		private class ViewHolder {
			public RelativeLayout row;
			public TextView name;
			public CheckBox checkbox;
		}
    }
    
	public void setFileName(List<String> files) {
		this.files = files;
	}

	public List<String> getFileNames() {
		return files;
	}	
}
