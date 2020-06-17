package kapilg99.android.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Messages> messagesList;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public MessageAdapter(List<Messages> messagesList) {
        this.messagesList = messagesList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
//        view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.my_msg, parent, false);
        if (viewType == 1) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.my_msg, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.their_msg, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        String currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        Messages message = messagesList.get(position);
        long time = message.getTime();
        SimpleDateFormat jdf = new SimpleDateFormat("dd-MM-yy h:mm a");
        jdf.setTimeZone(TimeZone.getDefault());
        String timeOfText = jdf.format(time);
        holder.timestamp.setText(timeOfText);
        holder.messageBody.setText(message.getMessage());
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    @Override
    public int getItemViewType(int position) {
        String currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        Messages m = messagesList.get(position);
        if (m.getFrom().equals(currentUserId)) {
            return 1;
        } else {
            return 0;
        }
//        return super.getItemViewType(position);
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        TextView messageBody;
        TextView timestamp;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageBody = itemView.findViewById(R.id.message_body);
            timestamp = itemView.findViewById(R.id.timestamp);
        }
    }
}
