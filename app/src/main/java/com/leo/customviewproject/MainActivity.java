package com.leo.customviewproject;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.leo.customviewproject.photoview.PhotoViewActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final List<Model> mModels = new ArrayList<>();

    {
        mModels.add(new Model(PhotoViewActivity.class, "PhotoView"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                final TextView textView = new TextView(parent.getContext());
                textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
                textView.setPadding(80, 50, 80, 50);
                textView.setTextColor(Color.BLACK);
                textView.setBackgroundColor(Color.WHITE);
                return new RecyclerView.ViewHolder(textView) {
                };
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                final Model model = mModels.get(position);
                if (holder.itemView instanceof TextView) {
                    final TextView textView = (TextView) holder.itemView;
                    textView.setText(model.name);
                    textView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(textView.getContext(), model.clazz);
                            startActivity(intent);
                        }
                    });
                }
            }

            @Override
            public int getItemCount() {
                return mModels.size();
            }
        });
    }

    private class Model {
        private final Class clazz;
        private final String name;

        Model(Class clazz, String name) {
            this.clazz = clazz;
            this.name = name;
        }
    }

}
