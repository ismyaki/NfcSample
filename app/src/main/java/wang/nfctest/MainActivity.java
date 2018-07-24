package wang.nfctest;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
	private String tag = "MainActivity";
	
	private PendingIntent mPendingIntent;
	private NfcAdapter nfcAdapter;
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
	private ListView lv;
	private ArrayAdapter arrayAdapter;
	private void initView(){
		cb_read_tech_list = findViewById(R.id.cb_read_tech_list);
		
		cb_read_data = findViewById(R.id.cb_read_data);
		
		lv = findViewById(R.id.lv);
		arrayAdapter = new ArrayAdapter<String>(
				this,
				R.layout.list_item,
				new ArrayList<String>());
		lv.setAdapter(arrayAdapter);
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
		if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
			processIntent(intent);
		}
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
		setText("-------READ END-------");
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
			for (int j = 0; j < MifareUltralight.PAGE_SIZE; j++) {
				byte[] data = mfu.readPages(j);
				setText("Page " + j + " : " + bytesToHexString(data));
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
					for (int i = 0; i < bCount; i++) {
						byte[] data = mfc.readBlock(bIndex);
						setText("Block " + bIndex + " : " + bytesToHexString(data));
						bIndex++;
					}
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

