package com.forcex.gui;
import com.forcex.gui.widgets.*;
import java.io.*;
import com.forcex.core.gpu.*;
import com.forcex.*;
import com.forcex.utils.*;
import java.util.*;

public class FileDialog {
	private Dialog dialog;
	private FileAdapter adapter;
	private File current;
	private String extension;
	private short selected = -1;
	private boolean modeFolder;
	private String title = "";
	private TextView tvpath;
	private short id;
	private ArrayList<ExtIcon> exticon;
	
	private class ExtIcon{
		String extension;
		int icon;
	}
	
	private FileDialog(UIContext ctx,String title,String path,final OnResultListener result,LanguageString lang,boolean modeFold,String ext){
		this.title = title;
		this.modeFolder = modeFold;
		this.extension = ext;
		exticon = new ArrayList<ExtIcon>();
		Layout lay = new Layout(ctx);
		Button parent = new Button(0.035f,0.035f);
		parent.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					browse(current.getParentFile(),(short)-1);
				}
		});
		parent.setIconTexture(Texture.load(FX.homeDirectory + "gui/parent.png"));
		lay.setToWrapContent();
		adapter = new FileAdapter(ctx);
		GridView gridview = new GridView(0.4f,0.25f,5,adapter);
		gridview.setApplyAspectRatio(true);
		gridview.setOnItemClickListener(new GridView.OnItemClickListener(){
				@Override
				public void onItemClick(Object item, short position, boolean longclick) {
					browse(((FileItem)item).file,position);
				}
		});
		parent.setApplyAspectRatio(true);
		lay.add(parent);
		tvpath = new TextView(ctx.default_font);
		tvpath.setAlignment(Layout.RIGHT);
		tvpath.setNoApplyConstraintY(true);
		tvpath.setTextSize(0.03f);
		lay.add(tvpath);
		lay.add(gridview);
		Button open = new Button(lang.get("accept"),ctx.default_font,0.1f,0.055f);
		open.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(!modeFolder && selected != -1 && adapter.getItem(selected).file.getName().endsWith(extension)){
						 result.open(id, adapter.getItem(selected).file.getAbsolutePath());
						 dialog.dismiss();
					}else if(modeFolder){
						result.open(id,current.getAbsolutePath());
						dialog.dismiss();
					}
				}
			});
		open.setAlignment(Layout.RIGHT);
		Button cancel = new Button(lang.get("cancel"),ctx.default_font,0.1f,0.055f);
		open.setDrawBorders(new Color(10,180,220));
		cancel.setDrawBorders(new Color(10,180,220));
		open.setMarginTop(0.02f);
		open.setTextSize(0.045f);
		cancel.setTextSize(0.045f);
		cancel.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(result != null && result.tryCancel(id)){
						dialog.dismiss();
					}
				}
			});
		cancel.setMarginTop(0.02f);
		cancel.setAlignment(Layout.RIGHT);
		lay.add(open);
		lay.add(cancel);
		browse(new File(path),(short)-1);
		dialog = new Dialog(lay);
		dialog.setOnDismissListener(new Dialog.OnDimissListener(){
				@Override
				public boolean dismiss() {
					if(result != null && result.tryCancel(id)){
						for(ExtIcon e : exticon){
							Texture.remove(e.icon);
						}
						return true;
					}
					return false;
				}
		});
		dialog.setIcon(Texture.load(FX.homeDirectory + "gui/fd_icon.png"));
		dialog.setTitle(title);
		dialog.show();
	}
	
	private void browse(File explore,short position){
		if(explore == null){
			Toast.error("Error:\nAccess Denieged.",4f);
			return;
		}
		if(explore.isDirectory()){
			current = explore;
			selected = -1;
			ArrayList<String> list = sort(explore);
			if(list != null){
				tvpath.setText(pathFormat(explore.getAbsolutePath()));
				adapter.removeAll();
				for(String filename : list){
					adapter.add(new FileItem(new File(current+"/"+filename)));
				}
			}else{
				Toast.error("Error:\nAccess Denieged.",4f);
			}
		}else{
			if(selected != -1){
				adapter.setSelect(selected,false);
			}
			adapter.setSelect(position,true);
			selected = position;
		}
	}
	
	public ArrayList<String> sort(File dir) {
		try{
			String[] list = dir.list();
			if(list == null){
				return null;
			}
			ArrayList<String> list_sorted = new ArrayList<>();
			ArrayList<String> directories = new ArrayList<>();
			ArrayList<String> files = new ArrayList<>();
			for(String l : list){
				if(new File(dir.getAbsolutePath() + "/" + l).isDirectory()){
					directories.add(l);
				}else{
					files.add(l);
				}
			}
			Collections.sort(directories);
			Collections.sort(files);
			for(String d : directories) {
				list_sorted.add(d);
			}
			if(!modeFolder){
				for(String d : files) {
					if(d.endsWith(extension)){
						list_sorted.add(d);
					}
				}
			}
			return list_sorted;
		}catch(Exception e){
			Logger.log(e);
		}
		return null;
	}
	
	private String pathFormat(String path){
		String[] split = path.split("/");
		String path_final = "";
		for(String s : split){
			if(s.length() < 10){
				path_final +=(s.length()>0? "/":"") + s;
			}else{
				path_final += "/"+s.substring(0,9) + "...";
			}
		}
		if(path_final.length() >= 70){
			path_final = "Very larger path";
		}
		return path_final;
	}
	
	public FileDialog addExtensionIcon(String extension,int icon){
		ExtIcon ei = new ExtIcon();
		ei.extension = extension;
		ei.icon = icon;
		exticon.add(ei);
		return this;
	}
	
	public static FileDialog create(UIContext context,String title,String path,String extension,OnResultListener result,LanguageString lang,int id){
		FileDialog fd = new FileDialog(context,title,path,result,lang,false,extension);
		fd.id = (short) id;
		return fd;
	}
	
	public static FileDialog create(UIContext context,String title,String path,OnResultListener result,LanguageString lang,int id){
		FileDialog fd = new FileDialog(context,title,path,result,lang,true,"");
		fd.id = (short)id;
		return fd;
	}
	
	public class FileItem {
		public File file;
		
		public FileItem(File file){
			this.file = file;
		}
	}
	
	private class FileAdapter extends GridAdapter<FileItem> {
		TextView tv;
		ImageView iv;
		int folder,unknown;
		
		public FileAdapter(UIContext ctx){
			super(ctx);
		}
		
		@Override
		protected void createView(Layout container) {
			tv = new TextView(getContext().default_font);
			unknown = Texture.load(FX.homeDirectory + "gui/file.png",false);
			folder = Texture.load(FX.homeDirectory + "gui/folder.png",false);
			tv.setTextSize(0.03f);
			tv.setConstraintWidth(container.getWidth());
			tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
			tv.setAlignment(Layout.CENTER);
			iv = new ImageView(-1,0.04f,0.048f);
			iv.setMarginTop(0.02f);
			iv.setApplyAspectRatio(true);
			iv.setAlignment(Layout.CENTER);
			container.add(iv);
			container.add(tv);
		}

		@Override
		protected void updateView(FileItem item, short position, Layout container) {
			String filename = item.file.getName();
			if(filename.length() > 20){
				filename = item.file.getName().substring(0,20)+"...";
			}
			tv.setText(filename);
			if(item.file.isDirectory()){
				iv.setTexture(folder);
			}else{
				for(ExtIcon ei : exticon){
					if(item.file.getName().endsWith(ei.extension)){
						iv.setTexture(ei.icon);
						return;
					}
				}
				iv.setTexture(unknown);
			}
		}

		@Override
		public void destroy() {
			iv.setTexture(-1);
			super.destroy();
			Texture.remove(unknown);
			Texture.remove(folder);
		}
	}
	
	public static interface OnResultListener{
		boolean tryCancel(short id);
		void open(short id,String path);
	}
}
