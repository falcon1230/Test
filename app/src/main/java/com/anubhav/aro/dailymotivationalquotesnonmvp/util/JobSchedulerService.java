package com.anubhav.aro.dailymotivationalquotesnonmvp.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.anubhav.aro.dailymotivationalquotesnonmvp.R;
import com.anubhav.aro.dailymotivationalquotesnonmvp.main.MainActivity;
import com.anubhav.aro.dailymotivationalquotesnonmvp.model.QuoteObject;
import com.anubhav.aro.dailymotivationalquotesnonmvp.model.Quotes;
import com.anubhav.aro.dailymotivationalquotesnonmvp.rest.ApiInterface;
import com.anubhav.aro.dailymotivationalquotesnonmvp.rest.ApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by anubhav on 20/03/18.
 */

public class JobSchedulerService extends JobService {

    private static List<Quotes> quotesList;
    private static final String CATEGORY = "inspire";
    private SharedPreferences sharedPreferences;
    private final String PREF_NAME = "SHARED_PREFS";


    public JobSchedulerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        showNotification();
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        new JobTask(this).execute(jobParameters);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }


    private class JobTask extends AsyncTask<JobParameters, Void, JobParameters> {

        private final JobService jobService;
        private SharedPreferences sharedPreferences;
        private SharedPreferences.Editor editor;

        public JobTask(JobService jobService) {
            this.jobService = jobService;
            sharedPreferences = getApplicationContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            editor = sharedPreferences.edit();
        }

        @Override
        protected JobParameters doInBackground(JobParameters... jobParameters) {
            ApiInterface apiInterface = ApiService.getRetrofit().create(ApiInterface.class);
            Call<QuoteObject> call = apiInterface.getQuote(CATEGORY);
            call.enqueue(new Callback<QuoteObject>() {
                @Override
                public void onResponse(Call<QuoteObject> call, Response<QuoteObject> response) {
                    //Toast.makeText(JobSchedulerService.this, "onResponse", Toast.LENGTH_SHORT).show();
                    if (response.isSuccessful()) {
                        quotesList = response.body().getContents().getQuotesList();
                        Quotes quotes = quotesList.get(0);
                        editor.putString("quote_title", quotes.getQuote());
                        editor.putString("quote_url", quotes.getBackground());
                        editor.putString("error_message", null);
                        editor.apply();
                    } else {
                        editor.putString("error_message", null);
                        editor.apply();
                    }
                }

                @Override
                public void onFailure(Call<QuoteObject> call, Throwable t) {
                    Toast.makeText(JobSchedulerService.this, "onFailure", Toast.LENGTH_SHORT).show();
                    editor.putString("quote_title", null);
                    editor.putString("quote_url", null);
                    editor.putString("error_message", null);
                    editor.apply();
                }
            });
            return jobParameters[0];
        }

        @Override
        protected void onPostExecute(JobParameters jobParameters) {
            super.onPostExecute(jobParameters);
            jobService.jobFinished(jobParameters, false);

        }
    }

    private void showNotification() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(getString(R.string.motivational_title))
                .setContentText(getString(R.string.motivation_body));

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = notificationBuilder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(0, notification);
    }
}
