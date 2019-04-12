package com.valdo.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

     ArrayList<String> titles = new ArrayList<>();
     ArrayList<String> content=new ArrayList<>();
      ArrayAdapter arrayAdapter;
      SQLiteDatabase articlesDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView=(ListView)findViewById(R.id.listView);
        arrayAdapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent=new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("content",content.get(position));

                startActivity(intent);

            }
        });





        //Setting up the database
        articlesDb=this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articlesDb.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR)");
        updateListView();



        //Setting up the download task that will download the data from de api
        DownloadTask task=new DownloadTask();
        try {
            //task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e)
        {
            e.printStackTrace();
        }

    }



      public void updateListView(){

          Cursor c = articlesDb.rawQuery("SELECT * FROM articles",null);
          int contentIndex = c.getColumnIndex("content");
          int titleIndex = c.getColumnIndex("title");

          //testing if we have any element (if c.moveTofirst())
          if(c.moveToFirst()){
              titles.clear();
              content.clear();
              do{
                  titles.add(c.getString(titleIndex));
                  content.add((c.getString(contentIndex)));

              }while(c.moveToNext()); // until we have a next element (until the end)
              arrayAdapter.notifyDataSetChanged();
          }


      }



      public class DownloadTask extends AsyncTask<String,Void,String>{


          @Override
          protected String doInBackground(String... strings) {

              String result="";
              URL url;
              HttpURLConnection urlConnection=null;

              try {
                  url=new URL(strings[0]);
                  urlConnection=(HttpURLConnection)url.openConnection();
                  InputStream in=urlConnection.getInputStream();
                  InputStreamReader reader=new InputStreamReader(in);
                  int data=reader.read();

                  while(data!= -1){

                      char current =(char) data;
                      result+=current;
                      data=reader.read();

                  }

                  JSONArray jsonArray=new JSONArray(result);

                  int numberOftimes=20;

                  if(jsonArray.length()<20){
                      numberOftimes=jsonArray.length();
                  }

                   articlesDb.execSQL("DELETE FROM articles");
                  for(int i=0;i<numberOftimes;i++){


                      String articleId=jsonArray.getString(i);

                      url=new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId+".json?print=pretty");

                      urlConnection=(HttpURLConnection)url.openConnection();
                      in =urlConnection.getInputStream();
                      reader=new InputStreamReader(in);
                      data=reader.read();
                      String articleInfo="";

                      while(data!= -1){

                          char current =(char) data;
                          articleInfo += current;
                          data = reader.read();

                      }

                   //   Log.i("Article info",articleInfo);
                      JSONObject jsonObject=new JSONObject(articleInfo);

                      if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){

                          String articleTitle=jsonObject.getString("title");
                          String articleUrl=jsonObject.getString("url");

                          //GEtting the data
                          url=new URL(articleUrl);
                          urlConnection=(HttpURLConnection)url.openConnection();
                          in =urlConnection.getInputStream();
                          reader=new InputStreamReader(in);
                          data=reader.read();
                          String articleContent="";

                          while(data!= -1){

                              char current =(char) data;
                              articleContent += current;
                              data = reader.read();

                          }
                            Log.i("Article content",articleContent);
                          String sql="INSERT INTO articles(articleId, title, content) VALUES(?, ?, ?)";
                          SQLiteStatement statement=articlesDb.compileStatement(sql);
                          statement.bindString(1,articleId);
                          statement.bindString(2,articleTitle);
                          statement.bindString(3,articleContent);
                          statement.execute();


                      }




                  }





              } catch (MalformedURLException e) {
                  e.printStackTrace();
              } catch (IOException e) {
                  e.printStackTrace();
              } catch (JSONException e) {
                  e.printStackTrace();
              }


              return null;
          }
          // this method is called when Download task is completed!
          @Override
          protected void onPostExecute(String s) {
              super.onPostExecute(s);
              updateListView();
          }
      }




}
