package wang.nfctest;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
	private String tag = "MainActivity";
	
	private PendingIntent mPendingIntent;
	private NfcAdapter nfcAdapter;
	
	private Tag myTag;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		needPermission();
		
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		
		initView();
		//
		if (checkSupportNfc() == true){
			setText("支援NFC");
			if(checkNfcOpen() == true){
				setText("NFC 已開啟");
			} else{
				setText("NFC 未開啟");
			}
		}else{
			setText("不支援NFC");
		}
		//
		mPendingIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
	}
	
	/**判斷是否支援 NFC*/
	public boolean checkSupportNfc(){
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if(nfcAdapter != null){
			return true;
		}else{
			return false;
		}
	}
	
	/**判斷 NFC 是否開啟*/
	public boolean checkNfcOpen(){
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if(nfcAdapter != null && nfcAdapter.isEnabled()){
			return true;
		}else{
			return false;
		}
	}
	
	private CheckBox cb_read_tech_list;
	private CheckBox cb_read_data;
	private CheckBox cb_read_ndef;
	private ListView lv;
	private ArrayAdapter arrayAdapter;
	private void initView(){
		cb_read_tech_list = findViewById(R.id.cb_read_tech_list);
		
		cb_read_data = findViewById(R.id.cb_read_data);
		
		cb_read_ndef = findViewById(R.id.cb_read_ndef);
		
		lv = findViewById(R.id.lv);
		arrayAdapter = new ArrayAdapter<String>(
				this,
				R.layout.list_item,
				new ArrayList<String>());
		lv.setAdapter(arrayAdapter);
		
		findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
//				write("123" , myTag);
			}
		});
	}
	
	private void setText(final String txt){
		Log.e(tag , "setText " + txt);
		SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
		String time = format.format(new Date());
		
		String str =  time + " " + txt;
		
		arrayAdapter.add(str);
		lv.smoothScrollToPosition(arrayAdapter.getCount()-1);
	}
	
	private final static int PERMISSIONS_REQUEST_ACCESS_COARSE = 12345;
	private boolean isNeedPermission = false;
	private String permissionString[] = {
			Manifest.permission.NFC,
	};
	/**
	 * 確認是否要請求權限(API > 23)
	 * API < 23 一律不用詢問權限
	 */
	private boolean needPermission() {
		if (Build.VERSION.SDK_INT >= 23) {
			ArrayList<String> list = new ArrayList<String>();
			for (String s : permissionString) {
				if (ContextCompat.checkSelfPermission(this, s) != PackageManager.PERMISSION_GRANTED) {
					list.add(s);
				}
			}
			if (list.size() > 0){
				isNeedPermission = true;
			}
			String p[] = new String[list.size()];
			for (int i = 0; i < list.size(); i++) {
				p[i] = list.get(i);
			}
			if (isNeedPermission == true) {
				ActivityCompat.requestPermissions(this, p, PERMISSIONS_REQUEST_ACCESS_COARSE);
				isNeedPermission = true;
			}
			return true;
		}else{
			isNeedPermission = false;
		}
		return false;
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == PERMISSIONS_REQUEST_ACCESS_COARSE) {
			boolean isAllTrue = true;
			for (String s : permissionString) {
				if (ContextCompat.checkSelfPermission(this, s) != PackageManager.PERMISSION_GRANTED) {
					isAllTrue = false;
					break;
				}
			}
			if (isAllTrue == false){
				isNeedPermission = true;
				needPermission();
			}else if (isAllTrue == true) {
				isNeedPermission = false;
			}
		}
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}
	
	private boolean isFirstRead = true;
	@Override
	protected void onResume() {
		super.onResume();
		nfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
		
		if (isFirstRead == true && NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent().getAction())) {
			//處理外部 intent
			processIntent(getIntent());
			isFirstRead = false;
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (nfcAdapter != null) {
			nfcAdapter.disableForegroundDispatch(this);
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())
				|| NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())
				|| NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
			processIntent(intent);
		}
	}
	
	private void write(String text, Tag tag) {
		//TODO 未完成
		NdefRecord[] records = { createRecord(text) };
		NdefMessage message = new NdefMessage(records);
		// Get an instance of Ndef for the tag.
		Ndef ndef = Ndef.get(tag);
		try {
			// Enable I/O
			ndef.connect();
			// Write the message
			ndef.writeNdefMessage(message);
			// Close the connection
			ndef.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FormatException e) {
			e.printStackTrace();
		}
	}
	
	private NdefRecord createRecord(String text){
		//TODO 未完成
		NdefRecord recordNFC = null;
		try {
			byte[] textBytes  = text.getBytes();
			byte[] langBytes  = Locale.getDefault().getLanguage().getBytes("UTF-8");
			int langLength = langBytes.length;
			int textLength = textBytes.length;
			byte[] payload = new byte[1 + langLength + textLength];
			
			// set status byte (see NDEF spec for actual bits)
			payload[0] = (byte) langLength;
			
			// copy langbytes and textbytes into payload
			System.arraycopy(langBytes, 0, payload, 1,              langLength);
			System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);
			
			recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT,  new byte[0], payload);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return recordNFC;
	}
	
	private String ByteArrayToHexString(byte[] inarray) {
		int i, j, in;
		String[] hex = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
		String out = "";
		for (j = 0; j < inarray.length; ++j) {
			in = (int) inarray[j] & 0xff;
			i = (in >> 4) & 0x0f;
			out += hex[i];
			i = in & 0x0f;
			out += hex[i];
		}
		return out;
	}
	
	//字符序列转换为16进制字符串
	private String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("0x");
		if (src == null || src.length <= 0) {
			return null;
		}
		char[] buffer = new char[2];
		for (int i = 0; i < src.length; i++) {
			buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
			buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
			System.out.println(buffer);
			stringBuilder.append(buffer);
		}
		return stringBuilder.toString();
	}
	
	/**
	 * Parses the NDEF Message from the intent and prints
	 */
	private void processIntent(Intent intent) {
		setText("------READ START------");
		//TODO 取出TAG ID
		setText("TAG ID : " + ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)));
		myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		//TODO 取出intent中的TAG
		if (cb_read_tech_list.isChecked() == true){
			readTechList(intent);
		}
		//TODO 取出資料
		if (cb_read_data.isChecked() == true){
			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			MifareUltralight mfu = MifareUltralight.get(tagFromIntent);
			if (mfu != null){
				readMifareUltralight(mfu);
			}
			
			MifareClassic mfc = MifareClassic.get(tagFromIntent);
			if (mfc != null){
				readMifareClassic(mfc);
			}
		}
		//TODO 取出 NDEF 資料
		if (cb_read_ndef.isChecked() == true){
			readNDEF2(intent);
		}
		setText("-------READ END-------");
	}
	
	/** 解析NDEF */
	public List<String> readNDEF2(Intent intent) {
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
			//取得 NdefMessage
			NdefMessage[] msgs = null;
			Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			
			//把序列化数据转成Messaeg对象
			if (rawMsgs != null) {
				msgs = new NdefMessage[rawMsgs.length];
				for (int i = 0; i < rawMsgs.length; i++) {
					msgs[i] = (NdefMessage) rawMsgs[i];
				}
			} else {
				// Unknown tag type
				byte[] empty = new byte[]{};
				NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
				NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
				msgs = new NdefMessage[]{msg};
			}
			//解析 NdefMessage
			for (NdefMessage ndefMessage : msgs) {
				NdefRecord[] records = ndefMessage.getRecords();
				int count = 0;
				for (int i = 0; i < records.length; i++) {
					NdefRecord ndefRecord = records[i];
					
					String tnfStr = "";
					if (ndefRecord.getTnf() == NdefRecord.TNF_EMPTY){
						tnfStr = "TNF_EMPTY";
					} else if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN){
						tnfStr = "TNF_WELL_KNOWN";
					} else if (ndefRecord.getTnf() == NdefRecord.TNF_MIME_MEDIA){
						tnfStr = "TNF_MIME_MEDIA";
					} else if (ndefRecord.getTnf() == NdefRecord.TNF_ABSOLUTE_URI){
						tnfStr = "TNF_ABSOLUTE_URI";
					} else if (ndefRecord.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE){
						tnfStr = "TNF_EXTERNAL_TYPE";
					} else if (ndefRecord.getTnf() == NdefRecord.TNF_UNKNOWN){
						tnfStr = "TNF_UNKNOWN";
					} else if (ndefRecord.getTnf() == NdefRecord.TNF_UNCHANGED){
						tnfStr = "TNF_UNCHANGED";
					}
					
					String typeStr = "";
					if (Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
						typeStr = "RTD_TEXT";
					} else if (Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_URI)) {
						typeStr = "RTD_URI";
					} else if (Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_SMART_POSTER)) {
						typeStr = "RTD_SMART_POSTER";
					} else if (Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_ALTERNATIVE_CARRIER)) {
						typeStr = "RTD_ALTERNATIVE_CARRIER";
					} else if (Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_HANDOVER_CARRIER)) {
						typeStr = "RTD_HANDOVER_CARRIER";
					} else if (Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_HANDOVER_REQUEST)) {
						typeStr = "RTD_HANDOVER_REQUEST";
					} else if (Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_HANDOVER_SELECT)) {
						typeStr = "RTD_HANDOVER_SELECT";
					}
					
					byte[] payload = ndefRecord.getPayload();
					count += payload.length;
					String payloadStr = "[";
					for (byte b : payload) {
						payloadStr += "" + b + ",";
					}
					payloadStr = payloadStr.substring(0 , payloadStr.length()-1) + "]";
					String str = parseString(ndefRecord);
//					str = new String(ndefRecord.getPayload());
					
					setText(i + " tnf : " + tnfStr);
					setText(i + " type : " + typeStr);
					if (str != null){
						setText(i + " str : " + str);
					} else if (ndefRecord.toUri() != null){
						setText(i + " uri : " + ndefRecord.toUri());
					} else if (ndefRecord.toMimeType() != null){
						setText(i + " mimeType : " + ndefRecord.toMimeType());
					}
					setText(i + " payload : " + payloadStr);
				}
				setText("payload count : " + count);
			}
		}
		return null;
	}
	
	/** 解析NFC信息 */
	public List<String> readNDEF(Intent intent) {
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
			NdefMessage[] msgs = getTagNdefMessage(intent);
			List<String> ndefList = getNdefString(msgs);
			
			if (ndefList != null && ndefList.size() != 0) {
				for (String s : ndefList) {
					setText(s);
				}
				return ndefList;
			}
		}
		return null;
	}
	
	/** 得到Intent中的NDEF数据 */
	private NdefMessage[] getTagNdefMessage(Intent intent) {
		NdefMessage[] msgs = null;
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		
		//把序列化数据转成Messaeg对象
		if (rawMsgs != null) {
			msgs = new NdefMessage[rawMsgs.length];
			for (int i = 0; i < rawMsgs.length; i++) {
				msgs[i] = (NdefMessage) rawMsgs[i];
			}
		} else {
			// Unknown tag type
			byte[] empty = new byte[]{};
			NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
			NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
			msgs = new NdefMessage[]{msg};
		}
		return msgs;
	}
	
	/** 把Message转成List */
	private List<String> getNdefString(NdefMessage[] msgs) {
		List<String> list = new ArrayList<>();
		if (msgs != null && msgs.length != 0) {
			for (NdefMessage ndefMessage : msgs) {
				list.addAll(parser(ndefMessage));
			}
			return list;
		}
		return null;
	}
	
	/** 把NDEF中的信息系转化为Record，并最终转化为String */
	private List<String> parser(NdefMessage ndefMessage) {
		NdefRecord[] records = ndefMessage.getRecords();
		List<String> elements = new ArrayList<>();
		int count = 0;
		for (NdefRecord ndefRecord : records) {
			String str = parseString(ndefRecord);
			if (str != null){
				elements.add(str);
			}
			
			byte[] payload = ndefRecord.getPayload();
			count += payload.length;
		}
		setText("payload count " + count);
		return elements;
	}
	
	/**
	 * 吧Ndef记录转为String，这里的格式是TNF_WELL_KNOWN with RTD_TEXT
	 * @param ndefRecord
	 * @return
	 */
	public static String parseString(NdefRecord ndefRecord) {
		if (ndefRecord.getTnf() != NdefRecord.TNF_WELL_KNOWN || !Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
			return null;
		}
		try {
			byte[] payload = ndefRecord.getPayload();
			//把byte转为String
			String textEncoding = ((payload[0] & 0200) == 0) ? "UTF-8" : "UTF-16";
			int languageCodeLength = payload[0] & 0077;
			String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
			String text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
			return text;
		} catch (UnsupportedEncodingException e) {
			// should never happen unless we get a malformed tag.
			return null;
		}
	}
	
	/**讀取 TechList*/
	private void readTechList(Intent intent){
		Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		for (String tech : tagFromIntent.getTechList()) {
			setText(tech);
		}
	}
	
	/**讀取 Data*/
	private void readMifareUltralight(MifareUltralight mfu){
		try {
			setText("讀取資料 MifareUltralight");
			//Enable I/O operations to the tag from this TagTechnology object.
			mfu.connect();
			int type = mfu.getType();
			String typeS = "";
			switch (type) {
				case MifareUltralight.TYPE_ULTRALIGHT:
					typeS = "TYPE_ULTRALIGHT";
					break;
				case MifareUltralight.TYPE_ULTRALIGHT_C:
					typeS = "TYPE_ULTRALIGHT_C";
					break;
				case MifareUltralight.TYPE_UNKNOWN:
					typeS = "TYPE_UNKNOWN";
					break;
				default:
					typeS = "未定義";
			}
			setText("Type ： " + typeS);
			setText("PageSize : " + MifareUltralight.PAGE_SIZE);
			int count = 0;
			for (int j = 0; j < MifareUltralight.PAGE_SIZE; j++) {
				byte[] data = mfu.readPages(j);
				count += data.length;
				setText("Page " + j + " : " + bytesToHexString(data));
			}
			setText("byte count " + count);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(tag , e.getMessage());
			setText(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(tag , e.getMessage());
			setText(e.getMessage());
		}
	}
	
	/**讀取 Data*/
	private void readMifareClassic(MifareClassic mfc){
		try {
			setText("讀取資料 MifareClassic");
			//Enable I/O operations to the tag from this TagTechnology object.
			mfc.connect();
			int type = mfc.getType();
			int sectorCount = mfc.getSectorCount();
			String typeS = "";
			switch (type) {
				case MifareClassic.TYPE_CLASSIC:
					typeS = "TYPE_CLASSIC";
					break;
				case MifareClassic.TYPE_PLUS:
					typeS = "TYPE_PLUS";
					break;
				case MifareClassic.TYPE_PRO:
					typeS = "TYPE_PRO";
					break;
				case MifareClassic.TYPE_UNKNOWN:
					typeS = "TYPE_UNKNOWN";
					break;
				default:
					typeS = "未定義";
			}
			setText("Type ： " + typeS);
			setText("SectorCount : " + sectorCount);
			setText("BlockCount : " + mfc.getBlockCount());
			setText("Size : " + mfc.getSize() + " Byte");
			int bCount;
			int bIndex;
			for (int j = 0; j < sectorCount; j++) {
				//Authenticate a sector with key A.
				boolean auth = mfc.authenticateSectorWithKeyA(j, MifareClassic.KEY_DEFAULT);
				if (auth == true) {
					setText("Sector " + j + ":驗證成功");
					// 读取扇区中的块
					bCount = mfc.getBlockCountInSector(j);
					bIndex = mfc.sectorToBlock(j);
					int count = 0;
					for (int i = 0; i < bCount; i++) {
						byte[] data = mfc.readBlock(bIndex);
						count += data.length;
						setText("Block " + bIndex + " : " + bytesToHexString(data));
						bIndex++;
					}
					setText("byte count " + count);
				} else {
					setText("Sector " + j + ":驗證失败");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(tag , e.getMessage());
			setText(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(tag , e.getMessage());
			setText(e.getMessage());
		}
	}
}

