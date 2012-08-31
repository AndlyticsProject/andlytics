package com.github.andlyticsproject.gwt;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import android.text.format.DateFormat;
import android.util.SparseArray;

import com.github.andlyticsproject.exception.DeveloperConsoleException;
import com.github.andlyticsproject.exception.NegativeIndexValueExecption;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.Comment;

public class GwtParser {

	private ArrayList<String> indexList;

	private ArrayList<String> valueList;

	private String jsonCopy;

	private static boolean DEBUG = false;
	private static boolean DEBUG_SHOW_IN_STRING = false;
	private static boolean DUMP_LISTS = false;

	private static final String TAG = GwtParser.class.getSimpleName();


	private static int dumpNumber = 0;

	public GwtParser(String json) {

		jsonCopy = json;

		if (DEBUG_SHOW_IN_STRING)
			for (int idx = 0; idx < json.length(); idx += 100) {
				System.out.println(json.substring(idx, Math.min(idx + 100, json.length())));
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
				;
			}

		// remove response prefix (//OK)
		json = json.replaceAll("//OK", "");

		// for large jsons there is a concat sometimes
		json = json.replace("].concat([", ",");
		try {
			ArrayList<String> idxList = new ArrayList<String>();
			ArrayList<String> valList = new ArrayList<String>();
			// XXX is this really needed?
			valList.add("null");
			JSONArray jsonArr = new JSONArray(json);
			for (int i = 0; i < jsonArr.length(); i++) {
				Object obj = jsonArr.get(i);
				if (obj instanceof JSONArray) {
					JSONArray valArr = jsonArr.getJSONArray(i);
					for (int j = 0; j < valArr.length(); j++) {
						valList.add(valArr.getString(j));
					}
					// XXX skip last two? elements
					break;
				} else {
					idxList.add(jsonArr.getString(i));
				}
			}

			Collections.reverse(idxList);
			setIndexList(idxList);
			setValueList(valList);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		if (DUMP_LISTS) {
			try {
				dumpToFile(jsonCopy, "jsonDump");
				List<LongIndexValue> longList = buildLongList();
				dumpToFile(toIndexedList(indexList), "indexList");
				dumpToFile(toIndexedList(valueList), "valueList");
				dumpToFile(longList, "longList");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			dumpNumber++;
		}
	}

	private static String toIndexedList(List<String> list) {
		StringBuilder buff = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			buff.append(String.format("[%d]=%s", i, list.get(i)));
			buff.append(",");
		}

		return buff.toString();
	}

	private static void dumpToFile(Object obj, String name) throws Exception {
		FileOutputStream fos = new FileOutputStream("/sdcard/" + name + dumpNumber + ".json");
		fos.write(obj.toString().getBytes("UTF-8"));
		fos.flush();
		fos.close();
	}


	private List<LongIndexValue> buildLongList() {
		int tokenCount = 0;

		List<LongIndexValue> longValues = new ArrayList<GwtParser.LongIndexValue>();

		int count = 0;
		// build list von longs
		for (int i = 0; i < indexList.size(); i++) {

			String string = indexList.get(i);

			boolean isJsonArray = string.startsWith("[") && tokenCount != 0;
			boolean isJsonLong = string.startsWith("'");

			if (isJsonLong) {
				Long value = Long.valueOf(decodeLong(string));
				// debugPrint("json long: index " + count + " value " + value);
				longValues.add(new LongIndexValue(i, value));
				count++;
			} else {
				/* - not needed if all app are in one json?
					                if (isJsonArray) {
					                    break;
					                }
					                */
			}

			tokenCount++;
		}

		return longValues;
	}

	public long getAppInfoSize() {

		long result = 0;

		String string = indexList.get(1);
		if (string.startsWith("'")) {
			result = decodeLong(string);
		} else
			result = Integer.parseInt(string);

		return result;
	}

	/*
	  1"java.util.ArrayList/3821976829",
	  18 size
	26:  2=com.google.wireless.android.vending.common.gwt.shared.UserComment/3904692143
	27: 15=Thank you!\tBest app ever! Using market/publish/Home in the browser was a real pain. This is MUCH better and shows the graphs :)
	28: 12=September 19, 2011
	29:  0=null
	30: 16=gaia:18272196544469197577:1:vm:13621562625926206828
	31:  5=gaia:02059867726298886742:1:vm:13621562625926206828
	32:  6=
	33: 17=Pash


	2:  2=com.google.wireless.android.vending.common.gwt.shared.UserComment/2951922129
	3:  3=Very useful\tWould be great if it'd support in app purchases.
	4:  4=December 13, 2011
	5:  0=null
	6:  1="java.util.ArrayList/4159755760
	7:  5=gaia:10753775737095900088:1:vm:13621562625926206828
	8:  5=gaia:10753775737095900088:1:vm:13621562625926206828
	9:  6=
	10:  7=Pigeon

	0 24:  2=com.google.wireless.android.vending.common.gwt.shared.UserComment/419123159
	1 25:  3=1.3.20
	2 26: 14=Excellent\tGood design and perfect solution for developers to follow his apps on market
	3 27: 15=January 5, 2012
	4 28:  0=null
	5 29:  0=null
	6 30: 16=GT-I9100
	7 31: 17=gaia:04161828387057606304:1:vm:13621562625926206828
	8 32:  5=January 9, 2012
	9 33:  8=
	10 34: 18=Esteban

	0 0 24:  2=com.google.wireless.android.vending.common.gwt.shared.UserComment/419123159
	1 18:  3=1.3.22
	2 19: 12=Thank u\tExcellent app.. helps me a lot
	3 20:'TULbHer'=2147483647
	4 21: 13=thunderg
	5 22: 14=LG Optimus One
	6 23: 15=gaia:13744373675861904959:1:vm:13621562625926206828
	7 24:  8=English
	8 25:  9=en
	9 26:  5=*****
	10 27:  0=null
	11 28:  0=null
	12 29:  0=null
	13 30: 10=
	14 31: 16=Sree Hari Reddy

	*/
	public List<Comment> getComments() {

		List<Comment> result = new ArrayList<Comment>();

		if (valueList.size() > 2) {

			// remove first two values from index array - is arraylist definition
			List<String> commentsIndexList = indexList.subList(2, indexList.size());
			Comment comment = new Comment();

			int commentNumber = 0;
			int commentIndex = -1;
			for (int i = 0; i < commentsIndexList.size(); i++) {

				String valueIndex = commentsIndexList.get(i);

				commentIndex++;

				switch (commentIndex) {
					case 0:
						comment = new Comment();
						break;
					case 1:

						if ("null".equals(getStringForIndex(valueIndex))) {
							comment.setAppVersion("");
						} else {
							comment.setAppVersion(getStringForIndex(valueIndex));
						}

						break;
					case 2:
						String text = getStringForIndex(valueIndex);
						text = text.replaceAll("\\\\\"", "\"");
						text = text.replaceAll("\\\\t", "\n");
						comment.setText(text);
						break;
					case 3:

						Date date = new Date();
						date.setTime(decodeLong(indexList.get(commentIndex + 2 + (commentNumber))));

						String dateString = DateFormat.format("EEEEE, d MMM yyyy", date).toString();

						comment.setDate(dateString);
						break;
					case 5:

						if ("null".equals(getStringForIndex(valueIndex))) {
							comment.setDevice("");
						} else {
							comment.setDevice(getStringForIndex(valueIndex));
						}

						break;

					case 9:
						comment.setRating(getIntForIndex(valueIndex));
						break;

					case 14:

						comment.setUser(getStringForIndex(valueIndex));
						commentIndex = -1;
						result.add(comment);

						commentNumber += 15;

						break;

					default:
						break;
				}
			}
		}

		return result;
	}

	public Map<String, Integer> getFeedbackOverview() {

		Map<String, Integer> result = new HashMap<String, Integer>();

		String appStart = "[,\\\"";
		String appEnd = "\\\",";
		String valueEnd = "]\\n";

		if (valueList.size() > 1) {

			String value = valueList.get(2);

			while (true) {

				int startIndex = value.indexOf(appStart);

				if (startIndex > 0) {

					value = value.substring(startIndex + appStart.length(), value.length());

					int appEndIndex = value.indexOf(appEnd);
					String appName = value.substring(0, appEndIndex);

					int valueEndIndex = value.indexOf(valueEnd);
					String number = value.substring(appEndIndex + appEnd.length(), valueEndIndex);

					result.put(appName, Integer.valueOf(number));

					value = value.substring(valueEndIndex + number.length(), value.length());

				} else {
					break;
				}
			}
		}

		return result;

	}

	private int getIntForIndex(String valueIndex) {
		return Integer.parseInt(valueIndex);
	}

	private String getStringForIndex(String valueIndex) {
		return valueList.get(Integer.parseInt(valueIndex));
	}

	public void setIndexList(ArrayList<String> indexList) {
		this.indexList = indexList;
	}

	public ArrayList<String> getIndexList() {
		return indexList;
	}

	public void setValueList(ArrayList<String> valueList) {
		this.valueList = valueList;
	}

	public ArrayList<String> getValueList() {
		return valueList;
	}

	public static boolean isValidResponse(String json) {
		return json == null || !json.startsWith("//OK");
	}

	public List<AppInfo> getAppInfos(String accountName) throws DeveloperConsoleException {

		List<AppInfo> result = new ArrayList<AppInfo>();

		try {

			Date now = new Date();
			SparseArray<LongIndexValue> activeInstallIndexMap = new SparseArray<LongIndexValue>();
			//   Map<Integer, LongIndexValue> commentsIndexMap = new HashMap<Integer, LongIndexValue>();
			SparseArray<LongIndexValue> fullAssetLongIndexMap = new SparseArray<LongIndexValue>();

			List<LongIndexValue> longValues = buildLongList();

			// number of apps in json
			int numberOfAppsInJson = 0;
			if (indexList.size() > 1) {
				numberOfAppsInJson = Integer.parseInt(indexList.get(1));
			}

			int longValueIndex = 0;

			debugPrint("number of apps: " + numberOfAppsInJson);
			debugPrint("number of long values: " + longValues.size());

			for (int i = 0; i < numberOfAppsInJson; i++) {

				if (longValues.size() > longValueIndex) {

					AppInfo info = new AppInfo();
					AppStats stats = new AppStats();
					stats.setRequestDate(now);

					// find ratings
					int ratingsStartIndex = findRatingsStartIndex(longValueIndex, longValues);

					// System.out.println("rating start:: " + ratingsStartIndex);
					// next is 0 ??? and then active installs

					// download is before ratings
					int totalDownloadIndex = ratingsStartIndex - 1;
					int totalDownloads = longValues.get(totalDownloadIndex).value.intValue();
					debugPrint("totalDownloads: " + totalDownloads);

					// after download there may be money elements
					int firstAfterMoney = findFirstAfterMoneyIndex(ratingsStartIndex - 1,
							longValues);

					// first before money is comments
					int comments = longValues.get(firstAfterMoney).value.intValue();
					stats.setTotalDownloads(totalDownloads);

					debugPrint("comments: " + comments);

					LongIndexValue activeInstallIndex = longValues.get(firstAfterMoney - 2);
					int activeInstalls = activeInstallIndex.value.intValue();
					activeInstallIndexMap.put(i, activeInstallIndex);
					stats.setActiveInstalls(activeInstalls);
					debugPrint("activeInstalls: " + activeInstalls);

					// total downloads is rating start -1

					//         commentsIndexMap.put(i, longValues.get(firstAfterMoney));

					if (firstAfterMoney >= 2) {
						fullAssetLongIndexMap.put(i, longValues.get(firstAfterMoney - 2));
					} else {
						// dummy to avlid npe
						debugPrint("full asses dummy - invalid value!!!");
						fullAssetLongIndexMap.put(i, longValues.get(firstAfterMoney));
					}

					stats.setNumberOfComments(comments);

					// set the ratings
					stats.setRating1(longValues.get(ratingsStartIndex).value.intValue());
					stats.setRating2(longValues.get(ratingsStartIndex + 1).value.intValue());
					stats.setRating3(longValues.get(ratingsStartIndex + 2).value.intValue());
					stats.setRating4(longValues.get(ratingsStartIndex + 3).value.intValue());
					stats.setRating5(longValues.get(ratingsStartIndex + 4).value.intValue());

					info.setLatestStats(stats);
					result.add(info);

					// move index to next element
					longValueIndex = (ratingsStartIndex + 4) + 1;

				}

			}

			List<AppInfo> draftElements = new ArrayList<AppInfo>();

			for (int j = 0; j < result.size(); j++) {

				AppInfo appInfo = result.get(j);

				/*
				4=com.google.wireless.android.vending.developer.shared.FullAssetInfo/4240394288
				'SD'=1155
				1=1
				5=com.google.wireless.android.vending.developer.shared.ApkInfo/2489460190
				*/

				int fullAssetLongIndex = activeInstallIndexMap.get(j).index;

				debugPrint("full asset long value: " + activeInstallIndexMap.get(j).value
						+ " (should be 0 or 'A')");
				debugPrint("full asset long index: " + fullAssetLongIndex);

				int apkinfoIndex = fullAssetLongIndex + 2;

				int apkinfoIndexFallback = fullAssetLongIndex + 1;

				// test for apk info element, if this is not a apk-info it's most likely a draft
				// app, skip it
				boolean isDraft = true;
				try {
					int parseInt = Integer.parseInt(indexList.get(apkinfoIndex));
					if (parseInt > 0 && parseInt < valueList.size()) {
						String apkString = valueList.get(parseInt);
						debugPrint("apkString : " + apkString);
						if (apkString.indexOf("ApkInfo") > 0) {
							isDraft = false;
						}
					}
				} catch (NumberFormatException e) {
					//Log.d("Andlytics", "skipping draft app, nfe.");
				}

				try {
					int parseInt = Integer.parseInt(indexList.get(apkinfoIndexFallback));
					if (parseInt > 0 && parseInt < valueList.size()) {
						String apkString = valueList.get(parseInt);
						debugPrint("apkString : " + apkString);
						if (apkString.indexOf("ApkInfo") > 0) {
							apkinfoIndex = apkinfoIndexFallback;
							isDraft = false;
						}
					}
				} catch (NumberFormatException e) {
					//Log.d("Andlytics", "skipping draft app, nfe.");
				}

				if (!isDraft) {

					// apk info is followed by apk manifest

					/*
					5=com.google.wireless.android.vending.developer.shared.ApkInfo/2489460190
					1	                6=com.google.wireless.android.vending.developer.shared.ApkManifest/1869115588
					3	                7=com.google.wireless.android.vending.developer.shared.Dimension/2931101581
					3	                8=com.google.common.base.Pair/1879869809
					9=java.lang.Integer/3438268394
					7=7
					9=java.lang.Integer/3438268394
					10000=10000
					0=null
					0=null
					0=null
					*/
					int intPairStartIndex = apkinfoIndex + 3;
					debugPrint("in pair start: " + getIndexStringValue(intPairStartIndex));
					int firstNullIndex = intPairStartIndex
							+ getIntegerPairLenght(intPairStartIndex);
					debugPrint("first null index (large integer): "
							+ getIndexIntegerValue(firstNullIndex));
					// This is new as of 2012/08/31. Not idea what's in the set 
					// seems to be empty (for now?)
					int setLength = getListOrSetLenght(firstNullIndex + 2, null);
					// 3 nulls + set length + next index (see below)
					int dimensionSetStart = firstNullIndex + 3 + setLength + 1;

					/*
					10000=10000
					0=null
					9=HashSet
					0=null
					0=null
					0=null

					10=java.util.HashSet/3273092938
					 16=size
					  8=com.google.common.base.Pair/1879869809
					 11=com.google.wireless.android.vending.developer.shared.Dimension$ScreenSize/2766144871
					  1=1
					 12=com.google.wireless.android.vending.developer.shared.Dimension$ScreenDensity/1170511186
					  3=3
					  8=com.google.common.base.Pair/1879869809
					-15=?
					-16=?
					*/
					SizeCallback dimensionPairLengthCallback = new SizeCallback() {

						@Override
						public int getElementLength(int startIndex) {

							/*
							try {
							    debugPrint("Pair?: " + getIndexStringValue(startIndex) + "  " + startIndex);
							} catch (NegativeIndexValueExecption e) {
							    // TODO Auto-generated catch block
							    e.printStackTrace();
							}
							*/

							int index = startIndex + 1;

							int firstDimensionInteger = getIndexIntegerValue(index);
							if (firstDimensionInteger < 1) {
								index++;
							} else {
								index += 2;
							}
							int secondDimensionInteger = getIndexIntegerValue(index);
							if (secondDimensionInteger < 1) {
								index++;
							} else {
								index += 2;
							}

							int lenght = index - startIndex;

							return lenght;
						}
					};

					debugPrint("dimension set start: " + getIndexStringValue(dimensionSetStart));
					int dimensionSetLength = getListOrSetLenght(dimensionSetStart,
							dimensionPairLengthCallback);

					debugPrint("hash set?: "
							+ getIndexStringValue(dimensionSetStart + dimensionSetLength)
							+ " index: " + (dimensionSetStart + dimensionSetLength));

					/*
					                    10=java.util.HashSet/3273092938
					                    0=null 7398
					                    0=null
					                   13=http://market.android.com/publish/images/PAAAAH43890gKGWWS7kWb6xrjkHd_pfCJ6LAg2pFp0qAQmYk1F27n04Ujq-nwfFsL1OqUlzp_RvFY3OEuFpd4ES7A3kAzfqVaXHgiKrUbcU0OaioJ_tQxwLebTII.png
					                   14=com.google.common.collect.RegularImmutableList/440499227
					*/

					int iconIndex = dimensionSetStart + dimensionSetLength + 3;
					if (getIndexStringValue(iconIndex) == null) {
						iconIndex++;
					}

					debugPrint("icon: " + getIndexStringValue(iconIndex) + " index: " + iconIndex);

					validateString(getIndexStringValue(iconIndex), "http", iconIndex);
					//iconIndex = secondListIndex + getListOrSetLenght(secondListIndex, null);
					appInfo.setIconUrl(getIndexStringValue(iconIndex));

					int permissionListStart = iconIndex + 1;
					int permissionListLength = getListOrSetLenght(permissionListStart, null);

					// 3 more list
					int postPermissionList1Start = permissionListStart + permissionListLength;
					int postPermissionList1Length = getListOrSetLenght(postPermissionList1Start,
							null);

					int postPermissionList2Start = postPermissionList1Start
							+ postPermissionList1Length;
					int postPermissionList2Length = getListOrSetLenght(postPermissionList2Start,
							null);

					// next is version code
					int productInfoIndex = postPermissionList2Start + postPermissionList2Length;
					// System.out.println("product info index " + productInfoIndex);
					//Integer versionCode = Integer.parseInt(indexList.get(versionCodeIndex));
					//debugPrint("product info element: " + getIndexStringValue(productInfoIndex));

					int nameIndex = productInfoIndex + 2;

					appInfo.setName(getIndexStringValue(nameIndex));
					debugPrint("app name: " + getIndexStringValue(nameIndex) + " index: "
							+ nameIndex);

					/*
					6889:174=1.3.30
					6890: 28=Andlytics
					6891:  0=null
					6892:-53=http://market.android.com/publish/images/PAAAAFDAn_NyNWInPtH1ZSOraup62FQVZ6P7_uN4-_B9as5tTHSUdrMGz9rCGmQGNoLZb9SlJ9Ls6KTfKDZ0lOCCLGsAzfqVadNEUvDgLdtJw3a3_XPGm5wG4lNi.png
					6893:  0=null
					6894:  0=null
					6895:-54=1.0.9
					6896: 30=com.github.andlyticsproject
					6897:163=380k
					*/

					/*
					 5212:  2=com.google.wireless.android.vending.developer.shared.ProductInfo/215520622
					 5213: 31=2.1.0
					 5214:430=Wood Bat
					 5215:  0=null
					 5216: 13=com.google.common.collect.RegularImmutableList/440499227
					 5217:  3=com.google.wireless.android.vending.developer.shared.ApkBundle/1372406882
					 5218:-1389=1390
					 5219:-1390=1391
					 5220:-1421=1422
					 5221:  0=null
					 5222:  0=null
					 5223:-48=439k
					 5224:433=com.rsoftr.android.woodbat
					 5225:437=2.7M
					 */

					// name is followed by:
					/*
					1. appname
					2. null
					3. null
					4. null
					5. null
					6. list
					7. packegename
					*/

					// it seems the first list has been removed as of 2012/08/31
					int listIndex = nameIndex + 4;
					// add list length
					int packageIndex = listIndex + getListOrSetLenght(listIndex, null);

					String packageName = getIndexStringValue(packageIndex);

					debugPrint("package name: " + packageName);
					validatePackageName(packageName);
					appInfo.setPackageName(packageName);

					appInfo.setLastUpdate(now);
					appInfo.setAccount(accountName);

					AppStats latestStats = appInfo.getLatestStats();
					latestStats.setVersionCode(0);
					appInfo.setLatestStats(latestStats);

					AppStats stats = appInfo.getLatestStats();
					debugPrint("number of comments " + stats.getNumberOfComments());
					debugPrint("number of installs " + stats.getActiveInstalls());
					debugPrint("number of downloads " + stats.getTotalDownloads());

					debugPrint("-- next app --" + j);

				} else {
					debugPrint("-- skip draft app --" + j);
					appInfo.setName("draft:" + j);
					appInfo.setDraftOnly(true);
					draftElements.add(appInfo);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new DeveloperConsoleException(jsonCopy, e);
		}

		return result;

	}

	private int findFirstAfterMoneyIndex(int downloadStartIndex, List<LongIndexValue> longValues) {

		LongIndexValue nextPotentialMoneyLong = longValues.get(downloadStartIndex - 1);
		int potentialMoneyElementIndex = nextPotentialMoneyLong.index - 1;

		try {
			int valueKey = getIntForIndex(indexList.get(potentialMoneyElementIndex));
			if (valueKey < valueList.size() && valueKey > -1) {

				String moneyString = valueList.get(valueKey);
				if (moneyString != null && moneyString.indexOf("SimpleMoney") > -1) {
					//debugPrint("money string found: " + moneyString);
					return findFirstAfterMoneyIndex(downloadStartIndex - 1, longValues);
				}
			}

		} catch (java.lang.NumberFormatException e) {
			// ignore
		}

		return downloadStartIndex - 1;
	}

	private void validatePackageName(String packageName) throws GwtParserException {

		if (packageName.indexOf('/') > -1) {
			throw new GwtParserException("error while parsing package name, found / : "
					+ packageName);
		}

		if (packageName.indexOf('.') < 0) {
			throw new GwtParserException("error while parsing package name, no '.' in name : "
					+ packageName);
		}

		if (packageName.indexOf(' ') > -1) {
			throw new GwtParserException("error while parsing package name, found space in name : "
					+ packageName);
		}

		if (packageName.startsWith("android.permission.")) {
			throw new GwtParserException(
					"error while parsing package name, found permission for name : " + packageName);
		}

		if (packageName.startsWith("android.hardware.")) {
			throw new GwtParserException(
					"error while parsing package name, found hardware for name : " + packageName);
		}

	}

	private int getIntegerPairLenght(int startIndex) throws GwtParserException {
		/*
		8=com.google.common.base.Pair/1879869809
		9=java.lang.Integer/3438268394
		4=4
		9=java.lang.Integer/3438268394
		10000=10000
		*/

		int lenght = 0;

		try {
			String pairString = getIndexStringValue(startIndex);
			validateString(pairString, "Pair", startIndex);
			lenght++;
		} catch (NegativeIndexValueExecption e) {
			return 1;
		}

		try {
			String integerString = getIndexStringValue(startIndex + lenght);
			validateString(integerString, "Integer", startIndex + lenght);
			lenght += 2;
		} catch (NegativeIndexValueExecption e) {
			lenght++;
		}

		try {
			String integerString = getIndexStringValue(startIndex + lenght);
			validateString(integerString, "Integer", startIndex + lenght);
			lenght += 2;
		} catch (NegativeIndexValueExecption e) {
			lenght++;
		}

		return lenght - 1;
	}

	private void validateString(String actual, String expected, int index)
			throws GwtParserException {

		if (actual.indexOf(expected) < 0) {
			throw new GwtParserException("expected: " + expected + " at index " + index
					+ " but found: " + actual);
		}

	}

	private void debugPrint(String string) {
		if (DEBUG) {
			System.out.println(string);
			//Log.d(TAG, string);
		}
	}

	/*
	private void printSaveValue(int list1) {

	    if(list1 < 0)
	        debugPrint(valueList.get(Integer.parseInt(indexList.get(list1))));
	    else
	        debugPrint(Integer.parseInt(indexList.get(list1)));
	    // TODO Auto-generated method stub

	}*/

	private int findRatingsStartIndex(int startIndex, List<LongIndexValue> longValues) {

		int ratingStart = startIndex;

		for (int i = startIndex; i < longValues.size() - 4; i++) {

			// find next 5 values with distance == 0
			long value1 = longValues.get(i).index;
			long value2 = longValues.get(i + 1).index;
			long value3 = longValues.get(i + 2).index;
			long value4 = longValues.get(i + 3).index;
			long value5 = longValues.get(i + 4).index;

			if (value5 == (value4 + 1) && value4 == (value3 + 1) && value3 == (value2 + 1)
					&& value2 == (value1 + 1)) {
				// found rating index in index array, return
				return i;
			}

		}

		return ratingStart;
	}

	protected int getMapLenght(int mapIndex, List<String> devconStringArray,
			ArrayList<String> indexList) {

		int lenght = 0;

		// read class name
		try {
			String string = getIndexStringValue(mapIndex);
			if (string.indexOf("Empty") > 0) {
				lenght = 1;
			} else if (string.indexOf("Singleton") > 0) {
				lenght = 5; // classname=1 +4xvalue
			} else {
				// filled map, read size
				int mapSize = getIndexIntegerValue(mapIndex + 2);
				//(3) == class name + boolean + sizeInt /FIXME add neg value check !!!
				lenght = 3 + (mapSize * 4);
			}

			return lenght;
		} catch (NegativeIndexValueExecption e) {
			return 1;
		}
	}

	private int getListOrSetLenght(int listIndex, SizeCallback callback) {

		int lenght = 0;

		// read class name
		try {

			String string = getIndexStringValue(listIndex);

			if (string == null) {
				return 1;
			}

			if (string.indexOf("Empty") > 0) {
				lenght = 1;
			} else if (string.indexOf("Singleton") > 0) {

				lenght = 3; // classname=1 +2xvalue

				// test for negative ref in singleton
				try {
					getIndexStringValue(listIndex + 1);
				} catch (NegativeIndexValueExecption e) {
					lenght = 2;
				}

			} else {
				// filled list or set, read size
				int listSize = getIndexIntegerValue(listIndex + 1);

				int valuesSize = 0;
				//(2) == class name + sizeInt
				int valuesStartIndex = listIndex + 2;
				for (int i = 0; i < listSize; i++) {

					int number = getIndexIntegerValue(valuesStartIndex + valuesSize);
					if (number < 0) {
						// negative is back reference, add one
						valuesSize++;
					} else {

						if (callback != null) {

							// for complex objects we need a callback
							valuesSize += callback.getElementLength(valuesStartIndex + valuesSize);
						} else {
							// standard element size is 2, simple objects like String values
							valuesSize += 2;
						}

					}

				}

				lenght = 2 + valuesSize;
			}

			return lenght;
		} catch (NegativeIndexValueExecption e) {
			return 1;
		}
	}

	private String getIndexStringValue(int index) throws NegativeIndexValueExecption {

		int indexValue = Integer.parseInt(indexList.get(index));

		if (indexValue == 0) {
			return null;
		} else if (indexValue < 0) {
			throw new NegativeIndexValueExecption();
		}

		return valueList.get(indexValue);
	}

	private int getIndexIntegerValue(int index) {

		String value = indexList.get(index);

		if (value.startsWith("[")) {
			return Integer.parseInt(value.substring(1));
		}

		if (value.endsWith("]")) {
			return Integer.parseInt(value.substring(0, value.length() - 1));
		}

		return Integer.parseInt(value);
	}

	protected long decodeLong(final String obfuscated) {

		String string = obfuscated.substring(1, obfuscated.length() - 1);
		long result = Base64Utils.longFromBase64(string);

		return result;
	}

	class LongIndexValue {
		public LongIndexValue(int i, Long value2) {
			index = i;
			value = value2;
		}

		public Long value;
		public int index;

		@Override
		public String toString() {
			return String.format("[%d -> %s]", index, value == null ? "null" : value.toString());
		}
	}

	interface SizeCallback {

		int getElementLength(int startIndex);

	}

}
