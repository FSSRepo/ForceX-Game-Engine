package com.forcex.gui;
import com.forcex.gui.widgets.*;
import java.io.*;
import com.forcex.core.gpu.*;
import com.forcex.*;
import com.forcex.utils.*;

public class FileDialog {
	private Dialog dialog;
	private GridAdapter adapter;
	private File current;
	String extension;
	private short selected = -1;
	boolean modeFolder;
	private String title = "";
	private TextView tvpath;
	short id;
	
	private FileDialog(UIContext ctx,String title,String path,final OnResultListener result,LanguageString lang){
		this.title = title;
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
		gridview.setOnItemClickListener(new GridView.onItemClickListener(){
				@Override
				public void onItemClick(Object item, short position, boolean longclick) {
					browse(((FileItem)item).file,position);
				}
		});
		parent.setApplyAspectRatio(true);
		lay.add(parent);
		tvpath = new TextView(ctx.default_font);
		tvpath.setAlignment(Layout.ALIGNMENT_RIGHT);
		tvpath.setNoApplyConstraintY(true);
		tvpath.setTextSize(0.03f);
		lay.add(tvpath);
		lay.add(gridview);
		Button open = new Button(lang.get("accept"),ctx.default_font,0.05f,0.035f);
		open.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(!modeFolder &&
						selected != -1 && 
						((FileItem)adapter.getItem(selected)).file.getName().endsWith(extension)){
						 result.open(id,((FileItem)adapter.getItem(selected)).file.getAbsolutePath());
						 dialog.dimiss();
					}else if(modeFolder){
						result.open(id,current.getAbsolutePath());
						dialog.dimiss();
					}
				}
			});
		open.setAlignment(Layout.ALIGNMENT_RIGHT);
		Button cancel = new Button(lang.get("cancel"),ctx.default_font,0.05f,0.035f);
		open.setUseEdge(new Color(10,180,220));
		cancel.setUseEdge(new Color(10,180,220));
		open.setTextSize(0.035f);
		cancel.setTextSize(0.035f);
		cancel.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					if(result.tryCancel(id)){
						dialog.dimiss();
					}
				}
			});
		open.setMarginTop(0.01f);
		cancel.setMarginTop(0.01f);
		cancel.setAlignment(Layout.ALIGNMENT_RIGHT);
		lay.add(open);
		lay.add(cancel);
		browse(new File(path),(short)-1);
		dialog = new Dialog(lay);
		dialog.setOnDimissListener(new Dialog.OnDimissListener(){
				@Override
				public boolean dimiss() {
					return result.tryCancel(id);
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
			String[] list = explore.list();
			if(list != null){
				tvpath.setText(explore.getAbsolutePath());
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
	
	public static FileDialog create(UIContext context,String title,String extension,String path,OnResultListener result,LanguageString lang,int id){
		FileDialog fd = new FileDialog(context,title,path,result,lang);
		fd.extension = extension;
		fd.id = (short) id;
		return fd;
	}
	
	public static FileDialog create(UIContext context,String title,String path,OnResultListener result,LanguageString lang,int id){
		FileDialog fd = new FileDialog(context,title,path,result,lang);
		fd.modeFolder = true;
		fd.id = (short)id;
		return fd;
	}
	
	private class FileItem{
		File file;
		
		public FileItem(File file){
			this.file = file;
		}
	}
	
	private class FileAdapter extends GridAdapter {
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
			tv.setAlignment(Layout.ALIGNMENT_CENTER);
			iv = new ImageView(-1,0.04f,0.048f);
			iv.setMarginTop(0.02f);
			iv.setApplyAspectRatio(true);
			iv.setAlignment(Layout.ALIGNMENT_CENTER);
			container.add(iv);
			container.add(tv);
		}

		@Override
		protected void updateView(Object item, short position, Layout container) {
			FileItem itm = (FileItem)item;
			tv.setText(itm.file.getName());
			if(itm.file.isDirectory()){
				iv.setTexture(folder);
			}else{
				iv.setTexture(unknown);
			}
		}
	}
	
	public static interface OnResultListener{
		boolean tryCancel(short id);
		void open(short id,String path);
	}
}
