package gr.thmmy.mthmmy.viewmodel;

import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import java.util.ArrayList;

import gr.thmmy.mthmmy.activities.settings.SettingsActivity;
import gr.thmmy.mthmmy.activities.topic.tasks.DeleteTask;
import gr.thmmy.mthmmy.activities.topic.tasks.EditTask;
import gr.thmmy.mthmmy.activities.topic.tasks.PrepareForEditResult;
import gr.thmmy.mthmmy.activities.topic.tasks.PrepareForEditTask;
import gr.thmmy.mthmmy.activities.topic.tasks.PrepareForReply;
import gr.thmmy.mthmmy.activities.topic.tasks.PrepareForReplyResult;
import gr.thmmy.mthmmy.activities.topic.tasks.ReplyTask;
import gr.thmmy.mthmmy.activities.topic.tasks.TopicTask;
import gr.thmmy.mthmmy.activities.topic.tasks.TopicTaskResult;
import gr.thmmy.mthmmy.base.BaseActivity;
import gr.thmmy.mthmmy.model.Post;
import gr.thmmy.mthmmy.session.SessionManager;

public class TopicViewModel extends BaseViewModel implements TopicTask.OnTopicTaskCompleted,
        PrepareForReply.OnPrepareForReplyFinished, PrepareForEditTask.OnPrepareEditFinished {
    /**
     * topic state
     */
    private boolean editingPost = false;
    private boolean writingReply = false;
    /**
     * A list of {@link Post#getPostIndex()} for building quotes for replying
     */
    private ArrayList<Integer> toQuoteList = new ArrayList<>();
    /**
     * caches the expand/collapse state of the user extra info in the current page for the recyclerview
     */
    private ArrayList<Boolean> isUserExtraInfoVisibile = new ArrayList<>();
    /**
     * holds the adapter position of the post being edited
     */
    private int postBeingEditedPosition;

    private TopicTask currentTopicTask;
    private PrepareForEditTask currentPrepareForEditTask;
    private PrepareForReply currentPrepareForReplyTask;

    //callbacks for topic activity
    private TopicTask.TopicTaskObserver topicTaskObserver;
    private DeleteTask.DeleteTaskCallbacks deleteTaskCallbacks;
    private ReplyTask.ReplyTaskCallbacks replyFinishListener;
    private PrepareForEditTask.PrepareForEditCallbacks prepareForEditCallbacks;
    private EditTask.EditTaskCallbacks editTaskCallbacks;
    private PrepareForReply.PrepareForReplyCallbacks prepareForReplyCallbacks;

    private MutableLiveData<TopicTaskResult> topicTaskResult = new MutableLiveData<>();
    private MutableLiveData<PrepareForReplyResult> prepareForReplyResult = new MutableLiveData<>();
    private MutableLiveData<PrepareForEditResult> prepareForEditResult = new MutableLiveData<>();

    private String firstTopicUrl;

    public void initialLoad(String pageUrl) {
        firstTopicUrl = pageUrl;
        currentTopicTask = new TopicTask(topicTaskObserver, this);
        currentTopicTask.execute(pageUrl);
    }

    public void loadUrl(String pageUrl) {
        stopLoading();
        currentTopicTask = new TopicTask(topicTaskObserver, this);
        currentTopicTask.execute(pageUrl);
    }

    public void reloadPage() {
        if (topicTaskResult.getValue() == null)
            throw new NullPointerException("No topic task has finished yet!");
        loadUrl(topicTaskResult.getValue().getLastPageLoadAttemptedUrl());
    }

    public void changePage(int pageRequested) {
        if (topicTaskResult.getValue() == null)
            throw new NullPointerException("No page has been loaded yet!");
        if (pageRequested != topicTaskResult.getValue().getCurrentPageIndex() - 1)
            loadUrl(topicTaskResult.getValue().getPagesUrls().get(pageRequested));
    }

    public void prepareForReply() {
        if (topicTaskResult.getValue() == null)
            throw new NullPointerException("Topic task has not finished yet!");
        stopLoading();
        changePage(topicTaskResult.getValue().getPageCount() - 1);
        currentPrepareForReplyTask = new PrepareForReply(prepareForReplyCallbacks, this,
                topicTaskResult.getValue().getReplyPageUrl());
        currentPrepareForReplyTask.execute(toQuoteList.toArray(new Integer[0]));
    }

    public void postReply(Context context, String subject, String reply) {
        if (prepareForReplyResult.getValue() == null) {
            throw new NullPointerException("Reply preparation was not found!");
        }
        PrepareForReplyResult replyForm = prepareForReplyResult.getValue();
        boolean includeAppSignature = true;
        SessionManager sessionManager = BaseActivity.getSessionManager();
        if (sessionManager.isLoggedIn()) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            includeAppSignature = prefs.getBoolean(SettingsActivity.POSTING_APP_SIGNATURE_ENABLE_KEY, true);
        }
        toQuoteList.clear();
        new ReplyTask(replyFinishListener, includeAppSignature).execute(subject, reply,
                replyForm.getNumReplies(), replyForm.getSeqnum(), replyForm.getSc(), replyForm.getTopic());
    }

    public void deletePost(String postDeleteUrl) {
        new DeleteTask(deleteTaskCallbacks).execute(postDeleteUrl);
    }

    public void prepareForEdit(int position, String postEditURL) {
        if (topicTaskResult.getValue() == null)
            throw new NullPointerException("Topic task has not finished yet!");
        stopLoading();
        currentPrepareForEditTask = new PrepareForEditTask(prepareForEditCallbacks, this, position,
                topicTaskResult.getValue().getReplyPageUrl());
        currentPrepareForEditTask.execute(postEditURL);
    }

    public void editPost(int position, String subject, String message) {
        if (prepareForEditResult.getValue() == null)
            throw new NullPointerException("Edit preparation was not found!");
        PrepareForEditResult editResult = prepareForEditResult.getValue();
        new EditTask(editTaskCallbacks, position).execute(editResult.getCommitEditUrl(), message,
                editResult.getNumReplies(), editResult.getSeqnum(), editResult.getSc(), subject, editResult.getTopic());
    }

    /**
     * cancel tasks that change the ui
     * topic, prepare for edit, prepare for reply tasks need to cancel all other ui changing tasks
     * before starting
     */
    public void stopLoading() {
        if (currentTopicTask != null && currentTopicTask.getStatus() == AsyncTask.Status.RUNNING) {
            currentTopicTask.cancel(true);
            topicTaskObserver.onTopicTaskCancelled();
        }
        if (currentPrepareForEditTask != null && currentPrepareForEditTask.getStatus() == AsyncTask.Status.RUNNING) {
            currentPrepareForEditTask.cancel(true);
            prepareForEditCallbacks.onPrepareEditCancelled();
        }
        if (currentPrepareForReplyTask != null && currentPrepareForReplyTask.getStatus() == AsyncTask.Status.RUNNING) {
            currentPrepareForReplyTask.cancel(true);
            prepareForReplyCallbacks.onPrepareForReplyCancelled();
        }
        // no need to cancel reply, edit and delete task, user should not have to wait for the ui
        // after he is done posting, editing or deleting
    }

    // callbacks for viewmodel
    @Override
    public void onTopicTaskCompleted(TopicTaskResult result) {
        topicTaskResult.setValue(result);
        if (result.getResultCode() == TopicTask.ResultCode.SUCCESS) {
            isUserExtraInfoVisibile.clear();
            for (int i = 0; i < result.getNewPostsList().size(); i++) {
                isUserExtraInfoVisibile.add(false);
            }
        }
    }

    @Override
    public void onPrepareForReplyFinished(PrepareForReplyResult result) {
        writingReply = true;
        prepareForReplyResult.setValue(result);
    }

    @Override
    public void onPrepareEditFinished(PrepareForEditResult result, int position) {
        editingPost = true;
        postBeingEditedPosition = position;
        prepareForEditResult.setValue(result);
    }

    // <-------------Just getters, setters and helper methods below here---------------->

    public boolean isUserExtraInfoVisible(int position) {
        return isUserExtraInfoVisibile.get(position);
    }

    public void hideUserInfo(int position) {
        isUserExtraInfoVisibile.set(position, false);
    }

    public void toggleUserInfo(int position) {
        isUserExtraInfoVisibile.set(position, !isUserExtraInfoVisibile.get(position));
    }

    public ArrayList<Integer> getToQuoteList() {
        return toQuoteList;
    }

    public void postIndexToggle(Integer postIndex) {
        if (toQuoteList.contains(postIndex))
            toQuoteList.remove(postIndex);
        else
            toQuoteList.add(postIndex);
    }

    public void setTopicTaskObserver(TopicTask.TopicTaskObserver topicTaskObserver) {
        this.topicTaskObserver = topicTaskObserver;
    }

    public void setDeleteTaskCallbacks(DeleteTask.DeleteTaskCallbacks deleteTaskCallbacks) {
        this.deleteTaskCallbacks = deleteTaskCallbacks;
    }

    public void setReplyFinishListener(ReplyTask.ReplyTaskCallbacks replyFinishListener) {
        this.replyFinishListener = replyFinishListener;
    }

    public void setPrepareForEditCallbacks(PrepareForEditTask.PrepareForEditCallbacks prepareForEditCallbacks) {
        this.prepareForEditCallbacks = prepareForEditCallbacks;
    }

    public void setEditTaskCallbacks(EditTask.EditTaskCallbacks editTaskCallbacks) {
        this.editTaskCallbacks = editTaskCallbacks;
    }

    public void setPrepareForReplyCallbacks(PrepareForReply.PrepareForReplyCallbacks prepareForReplyCallbacks) {
        this.prepareForReplyCallbacks = prepareForReplyCallbacks;
    }

    public MutableLiveData<TopicTaskResult> getTopicTaskResult() {
        return topicTaskResult;
    }

    public MutableLiveData<PrepareForReplyResult> getPrepareForReplyResult() {
        return prepareForReplyResult;
    }

    public MutableLiveData<PrepareForEditResult> getPrepareForEditResult() {
        return prepareForEditResult;
    }

    public void setEditingPost(boolean editingPost) {
        this.editingPost = editingPost;
    }

    public boolean isEditingPost() {
        return editingPost;
    }

    public int getPostBeingEditedPosition() {
        return postBeingEditedPosition;
    }

    public boolean canReply() {
        return topicTaskResult.getValue() != null && topicTaskResult.getValue().getReplyPageUrl() != null;
    }

    public boolean isWritingReply() {
        return writingReply;
    }

    public void setWritingReply(boolean writingReply) {
        this.writingReply = writingReply;
    }

    public String getBaseUrl() {
        if (topicTaskResult.getValue() != null) {
            return topicTaskResult.getValue().getBaseUrl();
        } else {
            return "";
        }
    }

    public String getTopicUrl() {
        if (topicTaskResult.getValue() != null) {
            return topicTaskResult.getValue().getLastPageLoadAttemptedUrl();
        } else {
            // topic task has not finished yet (log? disable menu button until load is finished?)
            return firstTopicUrl;
        }
    }

    public String getTopicTitle() {
        if (topicTaskResult.getValue() == null)
            throw new NullPointerException("Topic task has not finished yet!");
        return topicTaskResult.getValue().getTopicTitle();
    }

    public int getCurrentPageIndex() {
        if (topicTaskResult.getValue() == null)
            throw new NullPointerException("No page has been loaded yet!");
        return topicTaskResult.getValue().getCurrentPageIndex();
    }

    public int getPageCount() {
        if (topicTaskResult.getValue() == null)
            throw new NullPointerException("No page has been loaded yet!");

        return topicTaskResult.getValue().getPageCount();
    }

    public String getPostBeingEditedText() {
        if (prepareForEditResult.getValue() == null)
            throw new NullPointerException("Edit preparation was not found!");
        return prepareForEditResult.getValue().getPostText();
    }

    public String getBuildedQuotes() {
        if (prepareForReplyResult.getValue() != null) {
            return prepareForReplyResult.getValue().getBuildedQuotes();
        } else {
            return "";
        }
    }
}