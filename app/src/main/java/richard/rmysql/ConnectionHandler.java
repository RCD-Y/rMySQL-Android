package richard.rmysql;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import rMySQLCore.rMySQL;

public class ConnectionHandler {

	private View connPage;
	private rMySQL r;
	private Context context;
	private AlertDialog pendingDialog;
	private AlertDialog loadingScreen;
	private TextView[][] dataTexts;
	private EditText suggestedEditText;
	private float currentTotalWidth=0;
	private String previousSelect="";

	ConnectionHandler(View connPage, rMySQL r) {
		this.connPage=connPage;
		this.r=r;
		connPage.findViewById(R.id.connection_select).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectClicked();
			}
		});
		connPage.findViewById(R.id.connection_update).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateClicked("", "", "");
			}
		});
		connPage.findViewById(R.id.connection_result_expand).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				expandClicked();
			}
		});
		connPage.findViewById(R.id.connection_result_shrink).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				shrinkClicked();
			}
		});
	}

	public void setContext(Context context) {
		this.context=context;
	}

	public int getPixels(int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
	}

	public void showLoading(DialogInterface.OnClickListener listener) {
		AlertDialog.Builder builder=new AlertDialog.Builder(context);
		builder.setCancelable(false);
		builder.setTitle(R.string.dialog_please_wait);
		final ProgressBar loadingAnimation=new ProgressBar(context);
		loadingAnimation.setPadding(0,getPixels(20),0,0);
		builder.setView(loadingAnimation);
		builder.setNegativeButton(R.string.dialog_stop, listener);
		loadingScreen=builder.show();
	}

	public void dismissLoading() {
		loadingScreen.dismiss();
	}

	//region Suggestions

	public void suggestFields(final String tableName) {
		final AsyncTask<Void,Void,ArrayList<String>> asyncTask=new AsyncTask<Void,Void,ArrayList<String>>() {
			ArrayList<String> suggestedArray=new ArrayList<>();
			public ArrayList<String> doInBackground(Void... Params) {
				try {
					String[] result = r.RStoArray(r.QueryCustom("SHOW FIELDS FROM "+tableName))[0];
					for (String s:result) {
						suggestedArray.add(s);
					}
				} catch (SQLException e) {
					Toast.makeText(context,R.string.dialog_suggest_failed,Toast.LENGTH_SHORT).show();
				}
				return suggestedArray;
			}

			protected void onPostExecute(ArrayList<String> result) {
				AlertDialog.Builder builder=new AlertDialog.Builder(context);
				builder.setTitle("Field suggestion");
				ListView listView=new ListView(context);
				final ArrayAdapter<String> arrayAdapter=new ArrayAdapter<>(context, R.layout.list_item);
				arrayAdapter.addAll(result);
				listView.setAdapter(arrayAdapter);
				builder.setView(listView);
				dismissLoading();
				final AlertDialog choiceDialog=builder.show();
				listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						choiceDialog.dismiss();
						suggestedEditText.setText(arrayAdapter.getItem(position));
					}
				});
			}
		}.execute();
		showLoading(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				loadingScreen.dismiss();
				asyncTask.cancel(true);
			}
		});
	}

	public void suggestTables() {
		System.out.println(r.toString());
		final AsyncTask<Void,Void,ArrayList<String>> asyncTask=new AsyncTask<Void,Void,ArrayList<String>>() {
			ArrayList<String> suggestedArray=new ArrayList<>();
			public ArrayList<String> doInBackground(Void... params) {
				try {
					String[] result = r.RStoArray(r.QueryCustom("SHOW TABLES"))[0];
					for (String s:result) {
						suggestedArray.add(s);
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
				return suggestedArray;
			}

			protected void onPostExecute(ArrayList<String> result) {
				AlertDialog.Builder builder=new AlertDialog.Builder(context);
				builder.setTitle("Table suggestion");
				ListView listView=new ListView(context);
				final ArrayAdapter<String> arrayAdapter=new ArrayAdapter<>(context, R.layout.list_item);
				arrayAdapter.addAll(result);
				listView.setAdapter(arrayAdapter);
				builder.setView(listView);
				dismissLoading();
				final AlertDialog choiceDialog=builder.show();
				listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						choiceDialog.dismiss();
						suggestedEditText.setText(arrayAdapter.getItem(position));
					}
				});
			}
		}.execute();
		showLoading(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				loadingScreen.dismiss();
				asyncTask.cancel(true);
			}
		});
	}

	//endregion

	//region Results Operation
	public void putResults(String[] columnName, String[][] resultsArray) {
		final TableLayout resultList=(TableLayout) connPage.findViewById(R.id.connection_result_list);
		final TableRow topRow=new TableRow(context);
		dataTexts=new TextView[columnName.length][resultsArray[0].length+1];
		int padding=getPixels(1);
		for (int i=0; i<columnName.length; i++) {
			dataTexts[i][0]=new TextView(context);
			TextView dataText=dataTexts[i][0];
			dataText.setPadding(padding,padding,padding,padding);
			dataText.setText(columnName[i]);
			dataText.setSingleLine(false);
			dataText.setTypeface(null,Typeface.BOLD);
			dataText.setClickable(true);
			final String columnText=columnName[i];
			dataText.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					AlertDialog.Builder builder=new AlertDialog.Builder(context);
					builder.setTitle(R.string.connection_detail_data);
					builder.setMessage(columnText);
					builder.show();
				}
			});
			topRow.addView(dataText);
		}
		((MainActivity) context).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				resultList.removeAllViewsInLayout();
				resultList.addView(topRow);
			}
		});

		for (int i=0; i<resultsArray[0].length; i++) {
			final TableRow resultRow=new TableRow(context);
			for (int j=0; j<resultsArray.length; j++) {
				dataTexts[j][i+1]=new TextView(context);
				TextView dataText=dataTexts[j][i+1];
				dataText.setPadding(padding,padding,padding,padding);
				dataText.setClickable(true);
				dataText.setText(resultsArray[j][i]);
				dataText.setSingleLine(false);
				dataText.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						AlertDialog.Builder builder=new AlertDialog.Builder(context);
						builder.setTitle(R.string.connection_detail_data);
						builder.setMessage(((TextView) v).getText().toString());
						builder.show();
					}
				});
				dataText.setLongClickable(true);
				dataText.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						AlertDialog.Builder builder=new AlertDialog.Builder(context);
						builder.setTitle(R.string.connection_result_options);
						builder.show();
						return true;
					}
				});
				resultRow.addView(dataText);
			}
			((MainActivity) context).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					resultList.addView(resultRow);
				}
			});
		}
		adjustColumnWidth(connPage.getWidth()-getPixels(40));
	}

	public void adjustColumnWidth(float totalWidth) {
		currentTotalWidth=totalWidth;
		float widthLeft=totalWidth;
		for (int i=0; i<dataTexts.length; i++) {
			float tempWidth=widthLeft/(dataTexts.length-i);
			for (int j=0; j<dataTexts[0].length; j++) {
				dataTexts[i][j].setMaxWidth((int) tempWidth);
			}
			widthLeft-=tempWidth;
		}
	}

	public void expandClicked() {
		if (currentTotalWidth!=0) {
			adjustColumnWidth(currentTotalWidth*2);
		}
	}

	public void shrinkClicked() {
		if (currentTotalWidth!=0) {
			adjustColumnWidth(currentTotalWidth/2);
		}
	}
	//endregion

	//region UI Operations
	public void selectClicked() {
		AlertDialog.Builder builder=new AlertDialog.Builder(context);
		builder.setTitle(R.string.dialog_select_title);
		builder.setView(R.layout.dialog_select);
		builder.setPositiveButton(R.string.dialog_select_execute, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final AsyncTask asyncTask=new AsyncTask<String, Void, String>() {
					public String doInBackground(String... params) {
						try {
							ResultSet rs;
							String tempSelect;
							if (params[0].equals("true")) {
								rs=r.SearchCustom(params[1], params[2]);
								tempSelect=params[1]+";#;"+params[2];
							} else {
								rs=r.SearchForRecord(params[1], params[3], params[4]);
								tempSelect=params[1]+";;"+params[3]+";;"+params[4];
							}
							if (rs==null) {
								return "Nothing";
							}
							String[] metaData=new String[rs.getMetaData().getColumnCount()];
							for (int i=0; i<metaData.length; i++) {
								metaData[i]=rs.getMetaData().getColumnName(i+1);
							}
							putResults(metaData, r.RStoArray(rs));
							previousSelect=tempSelect;
							return "Success";
						} catch (SQLException e) {
							return e.getLocalizedMessage();
						}
					}

					public void onPostExecute(String result) {
						dismissLoading();
						if (result.equals("Nothing")) {
							AlertDialog.Builder subBuilder=new AlertDialog.Builder(context);
							subBuilder.setTitle(R.string.connection_operation_failed);
							subBuilder.setMessage(context.getString(R.string.connection_operation_nothing));
							subBuilder.show();
						} else if (!result.equals("Success")) {
							AlertDialog.Builder subBuilder=new AlertDialog.Builder(context);
							subBuilder.setTitle(R.string.connection_operation_failed);
							subBuilder.setMessage(context.getString(R.string.connection_operation_fail_message)+": "+result);
							subBuilder.show();
						}
					}
				}.execute(String.valueOf(((CheckBox) pendingDialog.findViewById(R.id.dialog_select_use_custom)).isChecked()),
						((EditText) pendingDialog.findViewById(R.id.dialog_select_table_name)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_select_custom_condition)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_select_flag_field)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_select_flag_value)).getText().toString());
				showLoading(new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						loadingScreen.dismiss();
						asyncTask.cancel(true);
					}
				});
			}
		});
		pendingDialog=builder.show();
		pendingDialog.findViewById(R.id.dialog_select_choice_table).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				suggestedEditText=(EditText) pendingDialog.findViewById(R.id.dialog_select_table_name);
				suggestTables();
			}
		});
		pendingDialog.findViewById(R.id.dialog_select_choice_field).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				suggestedEditText=(EditText) pendingDialog.findViewById(R.id.dialog_select_flag_field);
				suggestFields(((EditText) pendingDialog.findViewById(R.id.dialog_select_table_name)).getText().toString());
			}
		});
	}

	@SuppressWarnings("all")
	public void updateClicked(String tableName, String fieldName, String valueName) {
		AlertDialog.Builder builder=new AlertDialog.Builder(context);
		builder.setTitle(R.string.dialog_update_title);
		builder.setView(R.layout.dialog_update);
		builder.setPositiveButton(R.string.dialog_update_execute, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final AsyncTask asyncTask=new AsyncTask<String, Void, String>() {
					public String doInBackground(String... params) {
						try {
							if (params[0].equals("true")) {
								r.UpdateValueCustom(params[1],params[7],params[6]);
							} else {
								r.UpdateValue(params[1],params[2],params[3],params[4],params[5]);
							}
							return "Success";
						} catch (SQLException e) {
							return e.getLocalizedMessage();
						}
					}

					public void onPostExecute(String result) {
						dismissLoading();
						if (!result.equals("Success")) {
							AlertDialog.Builder subBuilder=new AlertDialog.Builder(context);
							subBuilder.setTitle(R.string.connection_operation_failed);
							subBuilder.setMessage(context.getString(R.string.connection_operation_fail_message)+": "+result);
							subBuilder.show();
						} else {
							AlertDialog.Builder subBuilder=new AlertDialog.Builder(context);
							subBuilder.setTitle(R.string.connection_operation_successful);
							subBuilder.setMessage(R.string.connection_operation_success_message);
							subBuilder.setPositiveButton(R.string.connection_operation_success_option_yes, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (previousSelect.contains(";")) redoSelect();
								}
							});
							subBuilder.setNegativeButton(R.string.connection_operation_success_option_no, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {}
							});
							subBuilder.show();
						}
					}
				}.execute(String.valueOf(((CheckBox) pendingDialog.findViewById(R.id.dialog_update_use_custom)).isChecked()),
						((EditText) pendingDialog.findViewById(R.id.dialog_update_table_name)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_update_flag_field)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_update_flag_value)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_update_dest_field)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_update_dest_value)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_update_custom_condition)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_update_custom_statement)).getText().toString());
				showLoading(new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						loadingScreen.dismiss();
						asyncTask.cancel(true);
					}
				});
			}
		});
		pendingDialog=builder.show();
		pendingDialog.findViewById(R.id.dialog_update_choice_table).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				suggestedEditText=(EditText) pendingDialog.findViewById(R.id.dialog_update_table_name);
				suggestTables();
			}
		});
		pendingDialog.findViewById(R.id.dialog_update_choice_field).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				suggestedEditText=(EditText) pendingDialog.findViewById(R.id.dialog_update_flag_field);
				suggestFields(((EditText) pendingDialog.findViewById(R.id.dialog_update_table_name)).getText().toString());
			}
		});
		pendingDialog.findViewById(R.id.dialog_update_choice_dest_field).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				suggestedEditText=(EditText) pendingDialog.findViewById(R.id.dialog_update_dest_field);
				suggestFields(((EditText) pendingDialog.findViewById(R.id.dialog_update_table_name)).getText().toString());
			}
		});
	}
	//endregion

	public void redoSelect() {
		final AsyncTask asyncTask=new AsyncTask<Void, Void, String>() {
			public String doInBackground(Void... params) {
				try {
					ResultSet rs;
					if (previousSelect.contains(";#;")) {
						rs=r.SearchCustom(previousSelect.split(";#;")[0],previousSelect.split(";#;")[1]);
					} else {
						rs=r.SearchForRecord(previousSelect.split(";;")[0],previousSelect.split(";;")[1],previousSelect.split(";;")[2]);
					}
					if (rs==null) {
						return "Nothing";
					}
					String[] metaData=new String[rs.getMetaData().getColumnCount()];
					for (int i=0; i<metaData.length; i++) {
						metaData[i]=rs.getMetaData().getColumnName(i+1);
					}
					putResults(metaData, r.RStoArray(rs));
					return "Success";
				} catch (SQLException e) {
					return e.getLocalizedMessage();
				}
			}

			public void onPostExecute(String result) {
				dismissLoading();
				if (result.equals("Nothing")) {
					AlertDialog.Builder subBuilder=new AlertDialog.Builder(context);
					subBuilder.setTitle(R.string.connection_operation_failed);
					subBuilder.setMessage(context.getString(R.string.connection_operation_nothing));
					subBuilder.show();
				} else if (!result.equals("Success")) {
					AlertDialog.Builder subBuilder=new AlertDialog.Builder(context);
					subBuilder.setTitle(R.string.connection_operation_failed);
					subBuilder.setMessage(context.getString(R.string.connection_operation_fail_message)+": "+result);
					subBuilder.show();
				}
			}
		}.execute();
		showLoading(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				loadingScreen.dismiss();
				asyncTask.cancel(true);
			}
		});
	}
}
