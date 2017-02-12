package io.singleton.wearcomplications;

public class Constants {

    static final String BASE_URL = "http://www.dial.fyi/";
    static final String USER_FRIENDLY_BASE_URL = "dial.fyi/";

    static final String REGISTER_PATH = "register_complications";
    static final String IMG_LIST_PATH = "imgs/%s";
    static final String FEED_PATH = "feed/%s";
    static final String CURL_PATH = "x/%s";

    static final String USER_CONFIG_PATH = "%s/%s";

    static String getRegisterUrl() {
        return BASE_URL + REGISTER_PATH;
    }

    static String getFeedUrl(String token) {
        return BASE_URL + String.format(FEED_PATH, token);
    }

    static String getCurlUrl(String token) {
        return BASE_URL + String.format(CURL_PATH, token);
    }
}
