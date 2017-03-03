package com.dynamsoft.tessocr;

//import android.os.Handler;
//import android.os.Message;
//import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;



/**
 * Created by AnWang on 16/5/20.
 */
public class Translate {

    private static final String BaiduTrans = "http://api.fanyi.baidu.com/api/trans/vip/translate";
    private static final String APP_ID = "20160520000021516";
    private static final String key = "otPwHzMQBBZH9P4dNlTw";
    private static final String salt = "1435660288";
    //private static final Random random = new Random();

//	private Handler insHandler = new Handler() {//Handler必须为static静态？不设为static会有黄色警告
//		@Override
//		public void handleMessage(Message msg) {
//			// TODO Auto-generated method stub
//			switch (msg.what) {
//				case 0:
//					String word = msg.getData().getString("word");
//                    ((TextView) findViewById(R.id.tv_result)).setText(word);
//					break;
//
//				default:
//					break;
//			}
//		}
//	};

    /**
     * 翻译
     */
    static String transEnTo(String result) {
        String putword = result;
        String str_return = new String("nochange?");
        String str = new String();
        String sign = new String();
        String back = new String();
        StringBuilder result111 = new StringBuilder();
        //int salt = random.nextInt(10000);
//        try {
//            // 对中文字符进行编码,否则传递乱码
//            putword = URLEncoder.encode(putword, "UTF-8");//这句到底前面就加还是后面才加！！！！？？？？？
//            if (putword != null){
//                return "result: " + result + "\n" + "putword: "+ putword;
//            }
//
//        } catch (Exception e) {
//            String textchongfu = "encode 错";
//            return textchongfu;
//        }

        try {
            str = APP_ID + putword + salt + key; //！！！！
            sign = MD5Util.getMD5String(str, "utf-8");

            putword = URLEncoder.encode(putword, "UTF-8"); //把这句话果然要移后面来！！！坑爹的百度！！！

            URL url = new URL(BaiduTrans + "?q=" + putword + "&from=en&to=zh&appid=" + APP_ID
                    + "&salt=" + salt +"&sign=" + sign);
//            if (sign != null){
//                return sign;
//            }
//            if (BaiduTrans != null) {
//                return BaiduTrans + "?q=" + putword + "&from=en&to=zh&appid=" + APP_ID
//                        + "&salt=" + salt + "&sign=" + sign; //将这个url放浏览器里试试
//            }  //不用测试了，url是对的！还有问题的话就是后面的问题了！！！！



            URLConnection con = url.openConnection();
            con.connect();
            InputStreamReader reader = new InputStreamReader(con.getInputStream());
            BufferedReader bufread = new BufferedReader(reader);
            StringBuffer buff = new StringBuffer();
            String line;
            while ((line = bufread.readLine()) != null) {
                return line;
                //buff.append(line);
            }
        } catch (Exception e) {
            String textchongfu = "第二部分错";
            return textchongfu;
        }


        try {
            JSONObject resultJson = new JSONObject(result111.toString());
            String error_code = resultJson.getString("from");
            if (error_code != null) {
                return ("出错代码:" + error_code + "from:" + resultJson.getString("to"));
            }

            JSONArray array = (JSONArray) resultJson.get("trans_result");
            JSONObject dst = (JSONObject) array.get(0);
            String text = dst.getString("dst");
            text = URLDecoder.decode(text, "utf-8");

        } catch (Exception e) {
            String text1 = "第三部分错";
            return text1;
        }




//        try {
//            JSONArray array = (JSONArray) resultJson.get("trans_result");
//            JSONObject dst = (JSONObject) array.get(0);
//            String text = dst.getString("dst");
//            text = URLDecoder.decode(text, "utf-8");
//
//            // 对字符进行解码
//            //str_return = JsonToString(result111.toString());
//
//            reader.close();
//            buffer.close();
//
//            //text = "根本不执行text？";
//            //return text;
//        } catch (Exception e) {
//            String text2 = "进入第二个Exception了？";
//            return text2;
//        }

////        try {
//            // 对中文字符进行编码,否则传递乱码
//            putword = URLEncoder.encode(putword, "UTF-8");//这句到底前面就加还是后面才加！！！！？？？？？
//
//            str = APP_ID + putword + salt + key; //！！！！
//            sign = MD5Util.getMD5String(str, "utf-8");
//
//            URL url = new URL(BaiduTrans + "?q=" + putword + "&from=en&to=zh&appid=" + APP_ID
//                    + "&salt=" + salt +"&sign=" + sign);
//
//            HttpURLConnection con = (HttpURLConnection)url.openConnection();//这里添加Http
//            con.connect();
//            InputStreamReader reader = new InputStreamReader(con.getInputStream(), "utf-8");
//            BufferedReader buffer = new BufferedReader(reader);
//            String line;
//            while ((line = buffer.readLine()) != null) {
//                result111.append(line).append("\n");
//            }
//
//
//            try {
//                JSONObject resultJson = new JSONObject(result111.toString());
//                String error_code = resultJson.getString("from");
//                if (error_code != null) {
//                    return ("出错代码:" + error_code + "from:" + resultJson.getString("to"));
//                }
//            } catch (Exception e) {
//                String text1 = "进入第一个Exception了？";
//                return text1;
//            }
//
//
//
//
//            try {
//                JSONArray array = (JSONArray) resultJson.get("trans_result");
//                JSONObject dst = (JSONObject) array.get(0);
//                String text = dst.getString("dst");
//                text = URLDecoder.decode(text, "utf-8");
//
//                // 对字符进行解码
//                //str_return = JsonToString(result111.toString());
//
//                reader.close();
//                buffer.close();
//
//                //text = "根本不执行text？";
//                //return text;
//            } catch (Exception e) {
//                String text2 = "进入第二个Exception了？";
//                return text2;
//            }
//
//
////        } catch (Exception e) {
////            // TODO Auto-generated catch block
////            e.printStackTrace();
////            String text1 = "进入Exception了？";
////            return text1;
////        }


        String last = "到了最后";
        return last;
    }


    /**
     * 获取jsoon中翻译的内容
     *
     * @param jstring
     * @return
     */
    static String JsonToString(String jstring) {
        try {
            JSONObject obj = new JSONObject(jstring);
            JSONArray array = obj.getJSONArray("trans_result");
            obj = array.getJSONObject(0);
            String word = obj.getString("dst");
            return word;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "";
    }
}
