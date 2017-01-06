package gr.thmmy.mthmmy.activities.profile.summary;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Objects;

import gr.thmmy.mthmmy.R;
import mthmmy.utils.Report;


/**
 * Use the {@link SummaryFragment#newInstance} factory method to create an instance of this fragment.
 */
public class SummaryFragment extends Fragment {
    /**
     * Debug Tag for logging debug output to LogCat
     */
    @SuppressWarnings("unused")
    private static final String TAG = "SummaryFragment";
    /**
     * The key to use when putting profile's source code String to {@link SummaryFragment}'s Bundle.
     */
    private static final String PROFILE_DOCUMENT = "PROFILE_DOCUMENT";
    /**
     * {@link ArrayList} of Strings used to hold profile's information. Data are added in
     * {@link SummaryTask}.
     */
    private ArrayList<String> parsedProfileSummaryData;
    /**
     * A {@link Document} holding this profile's source code.
     */
    private Document profileSummaryDocument;
    private SummaryTask summaryTask;
    private LinearLayout mainContent;

    public SummaryFragment() {
        // Required empty public constructor
    }

    /**
     * Use ONLY this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @param profileSummaryDocument a {@link Document} containing this profile's parsed page
     * @return A new instance of fragment Summary.
     */
    public static SummaryFragment newInstance(Document profileSummaryDocument) {
        SummaryFragment fragment = new SummaryFragment();
        Bundle args = new Bundle();
        args.putString(PROFILE_DOCUMENT, profileSummaryDocument.toString());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileSummaryDocument = Jsoup.parse(getArguments().getString(PROFILE_DOCUMENT));
        parsedProfileSummaryData = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.profile_fragment_summary, container, false);
        mainContent = (LinearLayout) rootView.findViewById(R.id.profile_activity_content);
        if (!parsedProfileSummaryData.isEmpty())
            populateLayout();
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (parsedProfileSummaryData.isEmpty()) {
            summaryTask = new SummaryTask();
            summaryTask.execute(profileSummaryDocument);
        }
        Report.d(TAG, "onActivityCreated");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (summaryTask != null && summaryTask.getStatus() != AsyncTask.Status.RUNNING)
            summaryTask.cancel(true);
    }

    /**
     * An {@link AsyncTask} that handles asynchronous parsing of a profile page's data.
     * {@link AsyncTask#onPostExecute(Object) OnPostExecute} method calls {@link #populateLayout()}
     * to build graphics.
     * <p>
     * <p>Calling SummaryTask's {@link AsyncTask#execute execute} method needs to have profile's url
     * as String parameter!</p>
     */
    public class SummaryTask extends AsyncTask<Document, Void, Void> {
        //Class variables
        /**
         * Debug Tag for logging debug output to LogCat
         */
        @SuppressWarnings("unused")
        private static final String TAG = "SummaryTask"; //Separate tag for AsyncTask

        protected Void doInBackground(Document... profileSummaryPage) {
            parsedProfileSummaryData = parseProfileSummary(profileSummaryPage[0]);
            return null;
        }

        protected void onPostExecute(Void result) {
            populateLayout();
        }

        /**
         * This method is used to parse all available information in a user profile.
         *
         * @param profile {@link Document} object containing this profile's source code
         * @return ArrayList containing this profile's parsed information
         * @see org.jsoup.Jsoup Jsoup
         */
        ArrayList<String> parseProfileSummary(Document profile) {
            //Method's variables
            ArrayList<String> parsedInformation = new ArrayList<>();

            //Contains all summary's rows
            Elements summaryRows = profile.select(".bordercolor > tbody:nth-child(1) > tr:nth-child(2) tr");

            for (Element summaryRow : summaryRows) {
                String rowText = summaryRow.text(), pHtml = "";

                if (summaryRow.select("td").size() == 1) //Horizontal rule rows
                    pHtml = "";
                else if (rowText.contains("Signature") || rowText.contains("Υπογραφή")) {
                    //This needs special handling since it may have css
                    { //Fix embedded videos
                        Elements noembedTag = summaryRow.select("noembed");
                        ArrayList<String> embededVideosUrls = new ArrayList<>();

                        for (Element _noembed : noembedTag) {
                            embededVideosUrls.add(_noembed.text().substring(_noembed.text()
                                            .indexOf("href=\"https://www.youtube.com/watch?") + 38
                                    , _noembed.text().indexOf("target") - 2));
                        }

                        pHtml = summaryRow.html();

                        int tmp_counter = 0;
                        while (pHtml.contains("<embed")) {
                            if (tmp_counter > embededVideosUrls.size())
                                break;
                            pHtml = pHtml.replace(
                                    pHtml.substring(pHtml.indexOf("<embed"), pHtml.indexOf("/noembed>") + 9)
                                    , "<div class=\"embedded-video\">"
                                            + "<a href=\"https://www.youtube.com/"
                                            + embededVideosUrls.get(tmp_counter) + "\" target=\"_blank\">"
                                            + "<img src=\"https://img.youtube.com/vi/"
                                            + embededVideosUrls.get(tmp_counter) + "/default.jpg\" alt=\"\" border=\"0\">"
                                            + "</a>"
                                            //+ "<img class=\"embedded-video-play\" src=\"http://www.youtube.com/yt/brand/media/image/YouTube_light_color_icon.png\">"
                                            + "</div>");
                        }
                    }

                    //Add stuff to make it work in WebView
                    //style.css
                    pHtml = ("<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\" />\n" +
                            "<div class=\"customSignature\">\n" + pHtml + "\n</div>");
                } else if (!rowText.contains("Name") && !rowText.contains("Όνομα")) { //Doesn't add username twice
                    if (Objects.equals(summaryRow.select("td").get(1).text(), ""))
                        continue;
                    //Style parsed information with html
                    pHtml = "<b>" + summaryRow.select("td").first().text() + "</b> "
                            + summaryRow.select("td").get(1).text();
                }
                parsedInformation.add(pHtml);
            }
            return parsedInformation;
        }
    }

    /**
     * Simple method that builds the UI of a {@link SummaryFragment}.
     * <p>Use this method <b>only after</b> parsing profile's data with
     * {@link gr.thmmy.mthmmy.activities.profile.ProfileActivity.ProfileTask} as it reads from
     * {@link #parsedProfileSummaryData}</p>
     */
    private void populateLayout() {
        for (String profileSummaryRow : parsedProfileSummaryData) {
            if (profileSummaryRow.contains("Signature")
                    || profileSummaryRow.contains("Υπογραφή")) { //This may contain css
                WebView signatureEntry = new WebView(this.getContext());
                signatureEntry.loadDataWithBaseURL("file:///android_asset/", profileSummaryRow,
                        "text/html", "UTF-8", null);
                mainContent.addView(signatureEntry);
                continue;
            }
            TextView entry = new TextView(this.getContext());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                entry.setTextColor(getResources().getColor(R.color.primary_text, null));
            } else {
                //noinspection deprecation
                entry.setTextColor(getResources().getColor(R.color.primary_text));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                entry.setText(Html.fromHtml(profileSummaryRow, Html.FROM_HTML_MODE_LEGACY));
            } else {
                //noinspection deprecation
                entry.setText(Html.fromHtml(profileSummaryRow));
            }

            mainContent.addView(entry);
        }
    }
}