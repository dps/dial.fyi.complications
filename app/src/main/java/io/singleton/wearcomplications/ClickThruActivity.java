package io.singleton.wearcomplications;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

public class ClickThruActivity extends Activity {

    private static final String TAG = "CTA";
    private TextView mTitleText;
    private TextView mBodyText;
    private NetworkImageView mNetworkImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_click_thru);

        mTitleText = (TextView) findViewById(R.id.titleText);
        mBodyText = (TextView) findViewById(R.id.bodyText);
        mNetworkImage = (NetworkImageView) findViewById(R.id.ctImage);


        mTitleText.setText(getIntent().getStringExtra(RssComplicationService.EXTRA_CT_TITLE));
        mBodyText.setText(getIntent().getStringExtra(RssComplicationService.EXTRA_CT_BODY) + "\n\n ");

        String url = getIntent().getStringExtra(RssComplicationService.EXTRA_CT_IMAGE);
        Log.d(TAG, "imageUrl:" + url);
        if (url != null) {
            mNetworkImage.setImageUrl(url, NetworkRequester.getInstance(this).getImageLoader());
        }

    }

}
