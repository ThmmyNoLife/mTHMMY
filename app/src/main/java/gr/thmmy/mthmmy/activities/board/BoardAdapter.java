package gr.thmmy.mthmmy.activities.board;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import gr.thmmy.mthmmy.R;
import gr.thmmy.mthmmy.activities.topic.TopicActivity;
import gr.thmmy.mthmmy.data.Board;
import gr.thmmy.mthmmy.data.Topic;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static gr.thmmy.mthmmy.activities.board.BoardActivity.BUNDLE_BOARD_TITLE;
import static gr.thmmy.mthmmy.activities.board.BoardActivity.BUNDLE_BOARD_URL;
import static gr.thmmy.mthmmy.activities.topic.TopicActivity.BUNDLE_TOPIC_TITLE;
import static gr.thmmy.mthmmy.activities.topic.TopicActivity.BUNDLE_TOPIC_URL;

/**
 * {@link RecyclerView.Adapter} that can display a {@link gr.thmmy.mthmmy.data.Board}.
 */
class BoardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "BoardAdapter";
    private final int VIEW_TYPE_SUB_BOARD_TITLE = 0;
    private final int VIEW_TYPE_SUB_BOARD = 1;
    private final int VIEW_TYPE_TOPIC_TITLE = 2;
    private final int VIEW_TYPE_TOPIC = 3;
    private final int VIEW_TYPE_LOADING = 4;

    private final Context context;
    private ArrayList<Board> parsedSubBoards = new ArrayList<>();
    private ArrayList<Topic> parsedTopics = new ArrayList<>();
    private final ArrayList<Boolean> boardExpandableVisibility = new ArrayList<>();
    private final ArrayList<Boolean> topicExpandableVisibility = new ArrayList<>();

    BoardAdapter(Context context, ArrayList<Board> parsedSubBoards, ArrayList<Topic> parsedTopics) {
        this.context = context;
        this.parsedSubBoards = parsedSubBoards;
        this.parsedTopics = parsedTopics;
    }

    interface OnLoadMoreListener {
        void onLoadMore();
    }

    @Override
    public int getItemViewType(int position) {
        if (position <= parsedSubBoards.size()) {
            if (position == 0) return VIEW_TYPE_SUB_BOARD_TITLE;
            return VIEW_TYPE_SUB_BOARD;
        } else if (position <= parsedSubBoards.size() + parsedTopics.size() + 1) {
            if (position == parsedSubBoards.size() + 1) return VIEW_TYPE_TOPIC_TITLE;
            if (parsedTopics.get(position - parsedSubBoards.size() - 1 - 1) != null)
                return VIEW_TYPE_TOPIC;
        }
        return VIEW_TYPE_LOADING;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SUB_BOARD_TITLE) {
            TextView subBoardTitle = new TextView(context);
            subBoardTitle.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT
                    , LinearLayout.LayoutParams.WRAP_CONTENT));
            subBoardTitle.setText(context.getString(R.string.child_board_title));
            subBoardTitle.setTypeface(subBoardTitle.getTypeface(), Typeface.BOLD);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                subBoardTitle.setBackgroundColor(context.getColor(R.color.card_background));
                subBoardTitle.setTextColor(context.getColor(R.color.accent));
            } else {
                //noinspection deprecation
                subBoardTitle.setBackgroundColor(context.getResources().getColor(R.color.card_background));
                //noinspection deprecation
                subBoardTitle.setTextColor(context.getResources().getColor(R.color.accent));
            }
            subBoardTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            subBoardTitle.setTextSize(20f);

            return new TitlesViewHolder(subBoardTitle);
        } else if (viewType == VIEW_TYPE_SUB_BOARD) {
            View subBoard = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.activity_board_sub_board, parent, false);
            return new SubBoardViewHolder(subBoard);
        } else if (viewType == VIEW_TYPE_TOPIC_TITLE) {
            TextView topicTitle = new TextView(context);
            topicTitle.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT
                    , LinearLayout.LayoutParams.WRAP_CONTENT));
            topicTitle.setText(context.getString(R.string.topic_title));
            topicTitle.setTypeface(topicTitle.getTypeface(), Typeface.BOLD);
            topicTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            topicTitle.setTextSize(20f);

            return new TitlesViewHolder(topicTitle);
        } else if (viewType == VIEW_TYPE_TOPIC) {
            View topic = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.activity_board_topic, parent, false);
            return new TopicViewHolder(topic);
        } else if (viewType == VIEW_TYPE_LOADING) {
            View loading = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.recycler_loading_item, parent, false);
            return new LoadingViewHolder(loading);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof SubBoardViewHolder) {
            final Board subBoard = parsedSubBoards.get(position - 1);
            final SubBoardViewHolder subBoardViewHolder = (SubBoardViewHolder) holder;

            if (boardExpandableVisibility.size() != parsedSubBoards.size()) {
                for (int i = boardExpandableVisibility.size(); i < parsedSubBoards.size(); ++i)
                    boardExpandableVisibility.add(false);
            }

            subBoardViewHolder.boardRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, BoardActivity.class);
                    Bundle extras = new Bundle();
                    extras.putString(BUNDLE_BOARD_URL, subBoard.getUrl());
                    extras.putString(BUNDLE_BOARD_TITLE, subBoard.getTitle());
                    intent.putExtras(extras);
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            });
            if (boardExpandableVisibility.get(subBoardViewHolder.getAdapterPosition() - 1)) {
                subBoardViewHolder.boardExpandable.setVisibility(View.VISIBLE);
                subBoardViewHolder.showHideExpandable.setImageResource(R.drawable.ic_arrow_drop_up);
            } else {
                subBoardViewHolder.boardExpandable.setVisibility(View.GONE);
                subBoardViewHolder.showHideExpandable.setImageResource(R.drawable.ic_arrow_drop_down);
            }
            subBoardViewHolder.showHideExpandable.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final boolean visible = boardExpandableVisibility.get(subBoardViewHolder.getAdapterPosition() - 1);
                    if (visible) {
                        subBoardViewHolder.boardExpandable.setVisibility(View.GONE);
                        subBoardViewHolder.showHideExpandable.setImageResource(R.drawable.ic_arrow_drop_down);
                    } else {
                        subBoardViewHolder.boardExpandable.setVisibility(View.VISIBLE);
                        subBoardViewHolder.showHideExpandable.setImageResource(R.drawable.ic_arrow_drop_up);
                    }
                    boardExpandableVisibility.set(subBoardViewHolder.getAdapterPosition() - 1, !visible);
                }
            });
            subBoardViewHolder.boardTitle.setText(subBoard.getTitle());
            subBoardViewHolder.boardMods.setText(subBoard.getMods());
            subBoardViewHolder.boardStats.setText(subBoard.getStats());
            subBoardViewHolder.boardLastPost.setText(subBoard.getLastPost());
            subBoardViewHolder.boardLastPost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, TopicActivity.class);
                    Bundle extras = new Bundle();
                    extras.putString(BUNDLE_TOPIC_URL, subBoard.getLastPostUrl());
                    //Doesn't put an already ellipsized topic title in Bundle
                    intent.putExtras(extras);
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            });
        } else if (holder instanceof TopicViewHolder) {
            final Topic topic = parsedTopics.get(position - parsedSubBoards.size() - 1 - 1);
            final TopicViewHolder topicViewHolder = (TopicViewHolder) holder;

            if (topicExpandableVisibility.size() != parsedTopics.size()) {
                for (int i = topicExpandableVisibility.size(); i < parsedTopics.size(); ++i)
                    topicExpandableVisibility.add(false);
            }

            topicViewHolder.topicRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, TopicActivity.class);
                    Bundle extras = new Bundle();
                    extras.putString(BUNDLE_TOPIC_URL, topic.getUrl());
                    extras.putString(BUNDLE_TOPIC_TITLE, topic.getSubject());
                    intent.putExtras(extras);
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            });
            if (topicExpandableVisibility.get(topicViewHolder.getAdapterPosition() - parsedSubBoards
                    .size() - 2)) {
                topicViewHolder.topicExpandable.setVisibility(View.VISIBLE);
                topicViewHolder.showHideExpandable.setImageResource(R.drawable.ic_arrow_drop_up);
            } else {
                topicViewHolder.topicExpandable.setVisibility(View.GONE);
                topicViewHolder.showHideExpandable.setImageResource(R.drawable.ic_arrow_drop_down);
            }
            topicViewHolder.showHideExpandable.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final boolean visible = topicExpandableVisibility.get(topicViewHolder.
                            getAdapterPosition() - parsedSubBoards.size() - 2);
                    if (visible) {
                        topicViewHolder.topicExpandable.setVisibility(View.GONE);
                        topicViewHolder.showHideExpandable.setImageResource(R.drawable.ic_arrow_drop_down);
                    } else {
                        topicViewHolder.topicExpandable.setVisibility(View.VISIBLE);
                        topicViewHolder.showHideExpandable.setImageResource(R.drawable.ic_arrow_drop_up);
                    }
                    topicExpandableVisibility.set(topicViewHolder.getAdapterPosition() -
                            parsedSubBoards.size() - 2, !visible);
                }
            });
            topicViewHolder.topicSubject.setTypeface(Typeface.createFromAsset(context.getAssets()
                    , "fonts/fontawesome-webfont.ttf"));
            String lockedSticky = topic.getSubject();
            if (topic.isLocked())
                lockedSticky += " " + context.getResources().getString(R.string.fa_lock);
            if (topic.isSticky()) {
                //topicViewHolder.topicSubject.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_pin, 0);
                lockedSticky += " " + context.getResources().getString(R.string.fa_sticky);
            }
            topicViewHolder.topicSubject.setText(lockedSticky);
            topicViewHolder.topicStartedBy.setText(context.getString(R.string.topic_started_by, topic.getStarter()));
            topicViewHolder.topicStats.setText(topic.getStats());
            topicViewHolder.topicLastPost.setText(context.getString(R.string.topic_last_post, topic.getLastPost()));
            topicViewHolder.topicLastPost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, TopicActivity.class);
                    Bundle extras = new Bundle();
                    extras.putString(BUNDLE_TOPIC_URL, topic.getLastPostUrl());
                    //Doesn't put an already ellipsized topic title in Bundle
                    intent.putExtras(extras);
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            });
        } else if (holder instanceof LoadingViewHolder) {
            LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
            loadingViewHolder.progressBar.setIndeterminate(true);
        }
    }

    @Override
    public int getItemCount() {
        int items = 0;
        if (parsedSubBoards != null) items += parsedSubBoards.size() + 1;
        if (parsedTopics != null) items += parsedTopics.size() + 1;
        return items;
    }

    private static class SubBoardViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout boardRow, boardExpandable;
        final TextView boardTitle, boardMods, boardStats, boardLastPost;
        final ImageButton showHideExpandable;

        SubBoardViewHolder(View board) {
            super(board);
            boardRow = (LinearLayout) board.findViewById(R.id.child_board_row);
            boardExpandable = (LinearLayout) board.findViewById(R.id.child_board_expandable);
            showHideExpandable = (ImageButton) board.findViewById(R.id.child_board_expand_collapse_button);
            boardTitle = (TextView) board.findViewById(R.id.child_board_title);
            boardMods = (TextView) board.findViewById(R.id.child_board_mods);
            boardStats = (TextView) board.findViewById(R.id.child_board_stats);
            boardLastPost = (TextView) board.findViewById(R.id.child_board_last_post);
        }
    }

    private static class TopicViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout topicRow, topicExpandable;
        final TextView topicSubject, topicStartedBy, topicStats, topicLastPost;
        final ImageButton showHideExpandable;

        TopicViewHolder(View topic) {
            super(topic);
            topicRow = (LinearLayout) topic.findViewById(R.id.topic_row_linear);
            topicExpandable = (LinearLayout) topic.findViewById(R.id.topic_expandable);
            showHideExpandable = (ImageButton) topic.findViewById(R.id.topic_expand_collapse_button);
            topicSubject = (TextView) topic.findViewById(R.id.topic_subject);
            topicStartedBy = (TextView) topic.findViewById(R.id.topic_started_by);
            topicStats = (TextView) topic.findViewById(R.id.topic_stats);
            topicLastPost = (TextView) topic.findViewById(R.id.topic_last_post);
        }
    }

    private static class TitlesViewHolder extends RecyclerView.ViewHolder {
        TitlesViewHolder(View title) {
            super(title);
        }
    }

    private static class LoadingViewHolder extends RecyclerView.ViewHolder {
        final MaterialProgressBar progressBar;

        LoadingViewHolder(View itemView) {
            super(itemView);
            progressBar = (MaterialProgressBar) itemView.findViewById(R.id.recycler_progress_bar);
        }
    }
}