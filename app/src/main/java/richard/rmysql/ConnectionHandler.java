package richard.rmysql;

import android.app.Activity;
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
import android.widget.LinearLayout;
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
	private AlertDialog pendingSubDialog;
	private AlertDialog loadingScreen;
	private String alteredTable;
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
		connPage.findViewById(R.id.connection_insert).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				insertClicked();
			}
		});
		connPage.findViewById(R.id.connection_remove).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deleteClicked("","","");
			}
		});
		connPage.findViewById(R.id.connection_create).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				createClicked();
			}
		});
		connPage.findViewById(R.id.connection_drop).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dropClicked();
			}
		});
		connPage.findViewById(R.id.connection_alter).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				alterClicked();
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
					((Activity) context).runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(context,R.string.dialog_suggest_failed,Toast.LENGTH_SHORT).show();
						}
					});
					return null;
				}
				return suggestedArray;
			}

			protected void onPostExecute(ArrayList<String> result) {
				if (result==null) {
					dismissLoading();
					return;
				}
				AlertDialog.Builder builder=new AlertDialog.Builder(context);
				builder.setTitle("Field suggestion");
				ListView listView=new ListView(context);
				final ArrayAdapter<String> arrayAdapter=new ArrayAdapter<>(context, R.layout.list_item_simple);
				arrayAdapter.addAll(result);
				listView.setAdapter(arrayAdapter);
				listView.setPadding(getPixels(8),getPixels(8),getPixels(8),getPixels(8));
				builder.setView(listView);
				dismissLoading();
				final AlertDialog choiceDialog=builder.show();
				listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						choiceDialog.dismiss();
						if (suggestedEditText==pendingDialog.findViewById(R.id.dialog_insert_fields)) {
							String historyText=suggestedEditText.getText().toString();
							if (historyText.endsWith(",")) {
								historyText+=" ";
							} else if ((!historyText.endsWith(", "))&&historyText.length()!=0) {
								historyText+=", ";
							}
							suggestedEditText.setText(historyText+arrayAdapter.getItem(position));
						} else {
							suggestedEditText.setText(arrayAdapter.getItem(position));
						}
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
				final ArrayAdapter<String> arrayAdapter=new ArrayAdapter<>(context, R.layout.list_item_simple);
				arrayAdapter.addAll(result);
				listView.setAdapter(arrayAdapter);
				listView.setPadding(getPixels(8),getPixels(8),getPixels(8),getPixels(8));
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
	public void putResults(final String[] columnName, String[][] resultsArray, final String tableName) {
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
				final String tempColumnName=columnName[j];
				final String tempDataText=resultsArray[j][i];
				dataText.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						AlertDialog.Builder builder=new AlertDialog.Builder(context);
						builder.setTitle(R.string.connection_result_options);
						builder.setView(R.layout.dialog_data_operation);
						final AlertDialog tempDialog=builder.show();
						tempDialog.findViewById(R.id.dialog_data_update).setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								updateClicked(tableName,tempColumnName,tempDataText);
								tempDialog.dismiss();
							}
						});
						tempDialog.findViewById(R.id.dialog_data_delete).setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								deleteClicked(tableName,tempColumnName,tempDataText);
								tempDialog.dismiss();
							}
						});
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
			final float tempWidth=widthLeft/(dataTexts.length-i);
			for (int j=0; j<dataTexts[0].length; j++) {
				final TextView tempText=dataTexts[i][j];
				((MainActivity) context).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						tempText.setMaxWidth((int) tempWidth);
					}
				});
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
							putResults(metaData, r.RStoArray(rs), params[1]);
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
							showSuccessDialog();
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
		((EditText) pendingDialog.findViewById(R.id.dialog_update_table_name)).setText(tableName);
		((EditText) pendingDialog.findViewById(R.id.dialog_update_flag_field)).setText(fieldName);
		((EditText) pendingDialog.findViewById(R.id.dialog_update_flag_value)).setText(valueName);
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

	public void insertClicked() {
		AlertDialog.Builder builder=new AlertDialog.Builder(context);
		builder.setTitle(R.string.dialog_insert_title);
		builder.setView(R.layout.dialog_insert);
		builder.setPositiveButton(R.string.dialog_insert_execute, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final AsyncTask asyncTask=new AsyncTask<String, Void, String>() {
					public String doInBackground(String... params) {
						try {
							r.InsertCustom(params[0],params[1],params[2]);
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
							showSuccessDialog();
						}
					}
				}.execute(((EditText) pendingDialog.findViewById(R.id.dialog_insert_table_name)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_insert_fields)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_insert_values)).getText().toString());
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
		pendingDialog.findViewById(R.id.dialog_insert_choice_table).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				suggestedEditText=(EditText) pendingDialog.findViewById(R.id.dialog_insert_table_name);
				suggestTables();
			}
		});
		pendingDialog.findViewById(R.id.dialog_insert_choice_fields).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				suggestedEditText=(EditText) pendingDialog.findViewById(R.id.dialog_insert_fields);
				suggestFields(((EditText) pendingDialog.findViewById(R.id.dialog_insert_table_name)).getText().toString());
			}
		});
	}

	@SuppressWarnings("all")
	public void deleteClicked(String tableName, String fieldName, String valueName) {
		AlertDialog.Builder builder=new AlertDialog.Builder(context);
		builder.setTitle(R.string.dialog_delete_title);
		builder.setView(R.layout.dialog_delete);
		builder.setPositiveButton(R.string.dialog_delete_execute, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final AsyncTask asyncTask=new AsyncTask<String, Void, String>() {
					public String doInBackground(String... params) {
						try {
							if (params[0].equals("true")) {
								r.DeleteRowCustom(params[1], params[2]);
							} else {
								r.DeleteRow(params[1], params[3], params[4]);
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
							showSuccessDialog();
						}
					}
				}.execute(String.valueOf(((CheckBox) pendingDialog.findViewById(R.id.dialog_delete_use_custom)).isChecked()),
						((EditText) pendingDialog.findViewById(R.id.dialog_delete_table_name)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_delete_custom_condition)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_delete_flag_field)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_delete_flag_value)).getText().toString());
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
		((EditText) pendingDialog.findViewById(R.id.dialog_delete_table_name)).setText(tableName);
		((EditText) pendingDialog.findViewById(R.id.dialog_delete_flag_field)).setText(fieldName);
		((EditText) pendingDialog.findViewById(R.id.dialog_delete_flag_value)).setText(valueName);
		pendingDialog.findViewById(R.id.dialog_delete_choice_table).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				suggestedEditText=(EditText) pendingDialog.findViewById(R.id.dialog_delete_table_name);
				suggestTables();
			}
		});
		pendingDialog.findViewById(R.id.dialog_delete_choice_field).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				suggestedEditText=(EditText) pendingDialog.findViewById(R.id.dialog_delete_flag_field);
				suggestFields(((EditText) pendingDialog.findViewById(R.id.dialog_delete_table_name)).getText().toString());
			}
		});
	}

	public void createClicked() {
		AlertDialog.Builder builder=new AlertDialog.Builder(context);
		builder.setTitle(R.string.dialog_create_table_title);
		builder.setView(R.layout.dialog_create_table);
		builder.setPositiveButton(R.string.dialog_create_table_execute, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final AsyncTask asyncTask=new AsyncTask<String, Void, String>() {
					public String doInBackground(String... params) {
						try {
							r.NewField(params[0],params[1],params[2],Integer.parseInt(params[3]));
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
							showSuccessDialog();
						}
					}
				}.execute(((EditText) pendingDialog.findViewById(R.id.dialog_create_table_table_name)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_create_table_field)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_create_table_type)).getText().toString(),
						((EditText) pendingDialog.findViewById(R.id.dialog_create_table_length)).getText().toString());
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
	}

	public void dropClicked() {
		AlertDialog.Builder builder=new AlertDialog.Builder(context);
		builder.setTitle(R.string.dialog_drop_title);
		builder.setView(R.layout.dialog_drop);
		builder.setPositiveButton(R.string.dialog_drop_execute, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final AsyncTask asyncTask=new AsyncTask<String, Void, String>() {
					public String doInBackground(String... params) {
						try {
							r.DropTable(params[0]);
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
							showNoSelectSuccessDialog();
						}
					}
				}.execute(((EditText) pendingDialog.findViewById(R.id.dialog_drop_table_name)).getText().toString());
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
		pendingDialog.findViewById(R.id.dialog_drop_choice_table).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				suggestedEditText=(EditText) pendingDialog.findViewById(R.id.dialog_drop_table_name);
				suggestTables();
			}
		});
	}

	public void alterClicked() {
		AlertDialog.Builder builder=new AlertDialog.Builder(context);
		builder.setTitle(R.string.dialog_alter_title);
		builder.setView(R.layout.dialog_alter);
		pendingDialog=builder.show();
		pendingDialog.findViewById(R.id.dialog_alter_choice_table).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				suggestedEditText=(EditText) pendingDialog.findViewById(R.id.dialog_alter_table_name);
				suggestTables();
			}
		});
		pendingDialog.findViewById(R.id.dialog_alter_load).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String tableString=((EditText) pendingDialog.findViewById(R.id.dialog_alter_table_name)).getText().toString();
				alteredTable=tableString;
				final AsyncTask asyncTask=new AsyncTask<Void, Void, String[][]>() {
					public String[][] doInBackground(Void... params) {
						try {
							return r.RStoArray(r.QueryCustom("SHOW FIELDS FROM "+tableString));
						} catch (SQLException e) {
							dismissLoading();
							AlertDialog.Builder subBuilder=new AlertDialog.Builder(context);
							subBuilder.setTitle(R.string.connection_operation_failed);
							subBuilder.setMessage(context.getString(R.string.connection_operation_fail_message)+": "+e.getLocalizedMessage());
							subBuilder.show();
							return null;
						}
					}

					public void onPostExecute(final String[][] result) {
						if (result == null) return;
						dismissLoading();
						((MainActivity) context).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								LinearLayout fieldList = (LinearLayout) pendingDialog.findViewById(R.id.dialog_alter_field_list);
								for (int i = 0; i < result[0].length; i++) {
									View fieldItem = ((MainActivity) context).getLayoutInflater().inflate(R.layout.list_item_field, fieldList);
									final String tempField = result[0][i];
									((TextView) fieldItem.findViewById(R.id.list_item_field_name)).setText(tempField);
									fieldItem.findViewById(R.id.list_item_field_edit).setOnClickListener(new View.OnClickListener() {
										@Override
										public void onClick(View v) {
											AlertDialog.Builder subBuilder=new AlertDialog.Builder(context);
											subBuilder.setTitle(R.string.dialog_alter_field_title_change);
											subBuilder.setView(R.layout.dialog_alter_field);
											subBuilder.setPositiveButton(R.string.dialog_alter_field_execute_change, new DialogInterface.OnClickListener() {
												@Override
												public void onClick(DialogInterface dialog, int which) {
													final AsyncTask subAsyncTask = new AsyncTask<String, Void, String>() {
														public String doInBackground(String... params) {
															try {
																r.CustomUpdate("ALTER TABLE "+tableString+" CHANGE "+tempField+" "+params[0]+" "+params[1]+" "+params[2]);
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
																dismissLoading();
																AlertDialog.Builder subBuilder = new AlertDialog.Builder(context);
																subBuilder.setTitle(R.string.connection_operation_successful);
																subBuilder.setMessage(context.getString(R.string.dialog_alter_successful));
																subBuilder.show();
															}
														}
													}.execute(((EditText) pendingSubDialog.findViewById(R.id.dialog_alter_field_name)).getText().toString(),
															((EditText) pendingSubDialog.findViewById(R.id.dialog_alter_field_type)).getText().toString(),
															((EditText) pendingSubDialog.findViewById(R.id.dialog_alter_field_extra)).getText().toString());
													showLoading(new DialogInterface.OnClickListener() {
														@Override
														public void onClick(DialogInterface dialog, int which) {
															loadingScreen.dismiss();
															subAsyncTask.cancel(true);
														}
													});
												}
											});
											pendingSubDialog=subBuilder.show();
										}
									});
									fieldItem.findViewById(R.id.list_item_field_remove).setOnClickListener(new View.OnClickListener() {
										@Override
										public void onClick(View v) {
											final AsyncTask subAsyncTask = new AsyncTask<Void, Void, Void>() {
												public Void doInBackground(Void... params) {
													try {
														r.DeleteField(tableString, tempField);
													} catch (SQLException e) {
														dismissLoading();
														AlertDialog.Builder subBuilder = new AlertDialog.Builder(context);
														subBuilder.setTitle(R.string.connection_operation_failed);
														subBuilder.setMessage(context.getString(R.string.connection_operation_fail_message) + ": " + e.getLocalizedMessage());
														subBuilder.show();
													}
													return null;
												}

												public void onPostExecute(Void result) {
													dismissLoading();
													AlertDialog.Builder subBuilder = new AlertDialog.Builder(context);
													subBuilder.setTitle(R.string.connection_operation_successful);
													subBuilder.setMessage(context.getString(R.string.dialog_alter_successful));
													subBuilder.show();
												}
											}.execute();
											showLoading(new DialogInterface.OnClickListener() {
												@Override
												public void onClick(DialogInterface dialog, int which) {
													loadingScreen.dismiss();
													subAsyncTask.cancel(true);
												}
											});
										}
									});
								}
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
		});
		pendingDialog.findViewById(R.id.dialog_alter_add).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder subBuilder=new AlertDialog.Builder(context);
				subBuilder.setTitle(R.string.dialog_alter_field_title_add);
				subBuilder.setView(R.layout.dialog_alter_field);
				subBuilder.setPositiveButton(R.string.dialog_alter_field_execute_add, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final AsyncTask subAsyncTask = new AsyncTask<String, Void, String>() {
							public String doInBackground(String... params) {
								try {
									r.CustomUpdate("CREATE TABLE "+alteredTable+" ("+params[0]+" "+params[1]+" "+params[2]+")");
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
									AlertDialog.Builder subBuilder = new AlertDialog.Builder(context);
									subBuilder.setTitle(R.string.connection_operation_successful);
									subBuilder.setMessage(context.getString(R.string.dialog_alter_successful));
									subBuilder.show();
								}
							}
						}.execute(((EditText) pendingSubDialog.findViewById(R.id.dialog_alter_field_name)).getText().toString(),
								((EditText) pendingSubDialog.findViewById(R.id.dialog_alter_field_type)).getText().toString(),
								((EditText) pendingSubDialog.findViewById(R.id.dialog_alter_field_extra)).getText().toString());
						showLoading(new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								loadingScreen.dismiss();
								subAsyncTask.cancel(true);
							}
						});
					}
				});
				pendingSubDialog=subBuilder.show();
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
					putResults(metaData, r.RStoArray(rs), previousSelect.split(";#;")[0]);
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

	public void showSuccessDialog() {
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

	public void showNoSelectSuccessDialog() {
		AlertDialog.Builder subBuilder=new AlertDialog.Builder(context);
		subBuilder.setTitle(R.string.connection_operation_successful);
		subBuilder.setMessage(R.string.connection_operation_successful);
		subBuilder.show();
	}
}
