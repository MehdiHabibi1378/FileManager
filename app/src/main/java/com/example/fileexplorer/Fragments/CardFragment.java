package com.example.fileexplorer.Fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fileexplorer.FileAdapter;
import com.example.fileexplorer.FileOpener;
import com.example.fileexplorer.OnFileSelectedListener;
import com.example.fileexplorer.R;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CardFragment extends Fragment implements OnFileSelectedListener {

    private FileAdapter fileAdapter;
    private RecyclerView recyclerView;
    private List<File> fileList;
    private ImageView img_back;
    private TextView tv_pathHolder;
    File storage;
    String secStorage;
    String data;
    String[] items = {"Details","Rename","Delete","Share"};

    View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_sdcard,container,false);

        tv_pathHolder = view.findViewById(R.id.tv_pathHolder);
        img_back = view.findViewById(R.id.img_back);

        File[] externalCacheDirs = getContext().getExternalCacheDirs();
        for (File file: externalCacheDirs){
            if (Environment.isExternalStorageRemovable(file)){
               secStorage = file.getPath().split("/Android")[0];
               break;
            }
        }

//        String cardStorage = System.getenv("SECONDARY_STORAGE");
//        String in = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        storage = new File(secStorage);

        try {
          data = getArguments().getString("path");
          File file = new File(data);
          storage = file;
        }catch (Exception e){
            e.printStackTrace();
        }

        tv_pathHolder.setText(storage.getAbsolutePath());
        runtimePermission();



        return view;
    }

    private void runtimePermission() {

        Dexter.withContext(getContext()).withPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                displayFiles();
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
    }

    public ArrayList<File> findFiles(File file){
       ArrayList<File> arrayList = new ArrayList<>();
       File[] files = file.listFiles();

       for (File file1 : files){
           if (file1.isDirectory() && !file1.isHidden()){
               arrayList.add(file1);
           }
       }

       for (File singleFile : files){
           if (singleFile.getName().toLowerCase().endsWith(".jpeg")||singleFile.getName().toLowerCase().endsWith(".jpg")||
                   singleFile.getName().toLowerCase().endsWith(".png")||singleFile.getName().toLowerCase().endsWith(".mp3")||
                   singleFile.getName().toLowerCase().endsWith(".wav")||singleFile.getName().toLowerCase().endsWith(".mp4")||
                   singleFile.getName().toLowerCase().endsWith(".pdf")||singleFile.getName().toLowerCase().endsWith(".doc")||
                   singleFile.getName().toLowerCase().endsWith(".mkv")||singleFile.getName().toLowerCase().endsWith(".apk")
           ){
               arrayList.add(singleFile);
           }
       }
       return arrayList;
    }

    private void displayFiles(){
        recyclerView = view.findViewById(R.id.recycler_internal);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(),1));
        fileList = new ArrayList<>();
        fileList.addAll(findFiles(storage));
        fileAdapter = new FileAdapter(getContext(),fileList,this);
        recyclerView.setAdapter(fileAdapter);
    }

    @Override
    public void onFileClicked(File file) {
        if(file.isDirectory()){
            Bundle bundle = new Bundle();
            bundle.putString("path",file.getAbsolutePath());
            CardFragment internalFragment = new CardFragment();
            internalFragment.setArguments(bundle);
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, internalFragment).addToBackStack(null).commit();

        }else {
            try {
                FileOpener.openFile(getContext(),file);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onFileLongClicked(File file,int posi) {
        final Dialog optionDialog = new Dialog(getContext());
        optionDialog.setContentView(R.layout.option_dialog);
        optionDialog.setTitle("Select Option");
        ListView options = (ListView) optionDialog.findViewById(R.id.list);
        ListAdapter listAdapter = new ListAdapter();
        options.setAdapter(listAdapter);
        optionDialog.show();

        options.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = items[position];

                switch (selectedItem){
                    case "Details":
                        AlertDialog.Builder detailDialog = new AlertDialog.Builder(getContext());
                        detailDialog.setTitle("Detail");
                        final TextView detail = new TextView(getContext());
                        detailDialog.setView(detail);
                        Date lastModified = new Date(file.lastModified());
                        SimpleDateFormat format = new SimpleDateFormat("dd/mm/yyyy");
                        String formatDate = format.format(lastModified);
                        detail.setText("File Name: "+file.getName()+"\n"+"Size: "+ Formatter.formatFileSize(getContext(),file.length())
                        +"\n"+"Path: "+file.getAbsolutePath()+"\n"+"Last Modified: "+formatDate);

                        detailDialog.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                optionDialog.cancel();
                            }
                        });
                        AlertDialog alertDialog = detailDialog.create();
                        alertDialog.show();
                        break;
                    case "Rename":
                        AlertDialog.Builder renameDialog = new AlertDialog.Builder(getContext());
                        renameDialog.setTitle("Rename File");
                        final EditText name = new EditText(getContext());
                        renameDialog.setView(name);

                        renameDialog.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String new_name = name.getEditableText().toString();
                                String extntion = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("."));
                                File current = new File(file.getAbsolutePath());
                                File destination = new File(file.getAbsolutePath().replace(file.getName(),new_name)+extntion);
                                if (current.renameTo(destination)){
                                    fileList.set(posi,destination);
                                    fileAdapter.notifyItemChanged(posi);
                                    Toast.makeText(getContext(), "Renamed", Toast.LENGTH_SHORT).show();

                                }else {
                                    Toast.makeText(getContext(), "couldn't Rename", Toast.LENGTH_SHORT).show();
                                }
                                optionDialog.cancel();
                            }
                        });

                        renameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                optionDialog.cancel();
                            }
                        });

                        AlertDialog alertDialog_rename = renameDialog.create();
                        alertDialog_rename.show();
                        break;
                    case "Delete":
                        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(getContext());
                        deleteDialog.setTitle("Delete"+file.getName()+"?");
                        deleteDialog.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (file.delete()) {

                                    fileList.remove(posi);
                                    fileAdapter.notifyDataSetChanged();
                                    Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                                }else {
                                    Toast.makeText(getContext(), "Can't Delete", Toast.LENGTH_SHORT).show();
                                }
                                optionDialog.cancel();
                            }

                        });
                        deleteDialog.setNegativeButton("no", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                optionDialog.cancel();
                            }
                        });

                        AlertDialog alertDialog_delete = deleteDialog.create();
                        alertDialog_delete.show();
                        break;
                    case "Share":
                        String filename = file.getName();
                        Intent share = new Intent();
                        share.setAction(Intent.ACTION_SEND);
                        share.setType("image/jpeg");
                        share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                        startActivity(Intent.createChooser(share,"Share "+filename));
                        break;


                }
            }
        });

    }

    class ListAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return items.length;
        }

        @Override
        public Object getItem(int position) {
            return items[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = getLayoutInflater().inflate(R.layout.option_layout,null);
            TextView txtOption = view.findViewById(R.id.txtOption);
            ImageView imgOption = view.findViewById(R.id.imgOption);
            txtOption.setText(items[position]);
            if (items[position].equals("Details")){
                imgOption.setImageResource(R.drawable.ic_details);
            }else if (items[position].equals("Rename")){
                imgOption.setImageResource(R.drawable.ic_folder);
            }else if (items[position].equals("Delete")){
                imgOption.setImageResource(R.drawable.ic_delete);
            }else if (items[position].equals("Share")){
                imgOption.setImageResource(R.drawable.ic_share);
            }
            return view;
        }
    }
}
