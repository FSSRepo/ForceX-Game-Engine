package com.forcex.gui.cmd;
import com.forcex.gui.*;
import com.forcex.utils.*;
import com.forcex.gui.widgets.*;
import java.util.*;
import com.forcex.app.threading.*;
import java.util.regex.*;
import com.forcex.*;
import java.io.*;
import com.forcex.io.*;
import com.forcex.app.*;

public class CommandLine extends View implements EditText.onEditTextListener
{
	Color background;
	TextView tvText;
	HashMap<String,Command> commands = new HashMap<>();
	HashMap<String,String> paths = new HashMap<>();
	String cmd_txt = "ForceX Console";
	EditText etInput;
	float text_size;
	ThreadTask cmd_thread;
	boolean running_cmd = false;
	boolean request_input = false;
	String name = "",response ="";
	String current_path = "";
	float cursorY = 0.0f;
	int numLines = 0,max_lines = 0;
	ArrayList<String> listed = new ArrayList<>();
	ArrayList<String> print_queue = new ArrayList<>();
	public static final byte RED = 0;
	public static final byte GREEN = 1;
	public static final byte BLUE = 2;
	public static final byte YELLOW = 3;
	public static final byte ORANGE = 4;
	
	public CommandLine(float width,float text_size,int max_lines) {
		setWidth(width);
		setHeight(max_lines * text_size);
		this.text_size = text_size;
		this.max_lines = max_lines;
		background = new Color(0,0,0);
		cmd_thread = new ThreadTask();
	}
	
	public void addCommand(String name,Command cmd) {
		commands.put(name,cmd);
	}
	
	public void addPath(String key,String value) {
		paths.put(key,value);
	}
	
	@Override
	public void onCreate(Drawer drawer) {
		addCommand("clear", new Command(){
				@Override
				public byte exec(CommandLine cmd, String[] args)
				{
					clear();
					return -2;
				}
		});
		addCommand("cat", new Command(){
				@Override
				public byte exec(CommandLine cmd, String[] args)
				{
					for(String arg : args){
						String[] param = arg.split(" ");
						String path = processPath(param[0]);
						if(new File(current_path + "/"+ path).exists()){
							printf(FileUtils.readStringText(current_path + "/" + path),-1);
						}else if(new File(path).exists()){
							printf(FileUtils.readStringText(path),-1);
						}else{
							printf("File not exist",RED);
						}
					}
					return 0;
				}
		});
		addCommand("cd", new Command(){
				@Override
				public byte exec(CommandLine cmd, String[] args)
				{
					for(String arg : args){
						String[] param = arg.split(" ");
						String path = processPath(param[0]).replace('_',' ');
						boolean error_dir = true;
						if(path.equals("..")){
							backPath();
							error_dir = false;
						}else if(new File(current_path+"/"+path).exists()){
							current_path += "/"+path;
							error_dir = false;
						}else if(new File(path).exists() && new File(path).isDirectory()){
							current_path = path;
							error_dir = false;
						}else{
							if(path.matches("(\\b(\\d*[.]?\\d+)\\b)")){
								int offset = Integer.parseInt(path);
								if(offset < listed.size() && new File(current_path + "/" + listed.get(offset)).isDirectory()){
									current_path = current_path + "/" + listed.get(offset);
									error_dir = false;
								}
							}
						}
						current_path = current_path.replace("//","/");
						if(!new File(current_path).isDirectory()){
							backPath();
						}
						if(error_dir){
							printf("'"+path+"'directory not exist.",RED);
							return 0;
						}else{
							listed.clear();
							printf(current_path,ORANGE);
						}
					}
					return 0;
				}
			});
		addCommand("ls", new Command(){
				@Override
				public byte exec(CommandLine cmd, String[] args) {
					File file = new File(current_path);
					if(current_path.length() > 0 && file.exists()){
						String[] filelist = file.list();
						listed.clear();
						for(String f : filelist){
							listed.add(f);
						}
						filelist = null;
						Collections.sort(listed); int offset = 0;
						for(String dir : listed){
							File f =  new File(current_path+"/"+dir);
							printf(offset+" "+f.getName()+" "+(f.isDirectory()?"[dir]":""),-1);
							offset ++;
						}
					}else{
						printf("Directory error",RED);
					}
					return 0;
				}
			});
		addCommand("mkdir", new Command(){
				@Override
				public byte exec(CommandLine cmd, String[] args)
				{
					for(String arg : args){
						String[] param = arg.split(" ");
						String path = param[0];
						if(current_path.length() == 0){
							printf("There isn't current path",RED);
							return Command.CMD_ERROR;
						}else if(new File(current_path+"/"+path).exists()){
							printf("Directory already exist",RED);
							return Command.CMD_ERROR;
						}else{
							new File(current_path+"/"+path).mkdir();
							printf("Directory created.",-1);
						}
					}
					return 0;
				}
			});
		tvText = new TextView(context.default_font);
		tvText.setTextColor(255,255,255);
		tvText.setTextSize(text_size);
		tvText.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
		etInput = new EditText(context,1.0f,text_size,text_size);
		etInput.setBackground(0,0,0,true); etInput.setTextColor(255,255,255);
		etInput.setEdgeEnabled(false); etInput.setCursorStyle((byte)2);
		etInput.setAutoFocus(true);
		etInput.setOnEditTextListener(this);
		etInput.addFilter(240,110,34,"(\\b(\\d*[.]?\\d+)\\b)");
		String filter = "(?<=\\b)(";
		int off = 0;
		for(String cmd : commands.keySet()){
			filter += "("+cmd+")"+(off != commands.size() - 1 ? "|" : "");
			off++;
		}
		filter += ")(?=\\b)";
		etInput.addFilter(0,180,210,filter);
		paths.put("fxdir",FX.homeDirectory.substring(0,FX.homeDirectory.length()-1));
		String[] pths = new String[paths.size()];
		off = 0;
		for(String v : paths.keySet()){
			pths[off] = v;
			off++;
		}
		etInput.addFilter(190,190,0,pths);
		cmd_thread.start();
		rewindInput();
	}
	
	boolean first = true;
	float oy;
	
	@Override
	public void onTouch(float x, float y, byte type) {
		if(GameUtils.testRect(x,y,etInput.local,etInput.getWidth(),etInput.getHeight())){
			etInput.onTouch(x,y,type);
			return;
		}else{
			KeyBoard kb = etInput.getKeyboard();
			if(kb != null && !kb.testTouch(x,y)){
				etInput.detachKeyBoard();
			}else{
				if(first){
					oy = y;
					first = false;
				}else if(numLines > max_lines && type == EventType.TOUCH_DRAGGING){
					float delta = (y - oy) * 1.25f;
					if(cursorY+delta > 0.0f && cursorY + delta < ((numLines - max_lines) + 2) * text_size* 2f){
						cursorY += delta;
					}
				}
			}
		}
		oy = y;
	}

	@Override
	protected boolean testTouch(float x, float y) {
		return GameUtils.testRect(x,y,local,extent.x,extent.y);
	} 
	
	public String getInput(String name){
		printf(name,-1);
		etInput.setAutoFocus(true);
		this.name = name;
		request_input = true;
		while(request_input){
			try {
				Thread.sleep(100);
			}catch (InterruptedException e){}
		}
		return response;
	}
	
	private void updateListText(String cmd) {
		float t_width = 0.0f;
		String[] lines = cmd.split("\n");
		String txt_temp = "";
		for(int j = 0;j < lines.length;j++){
			String[] words = lines[j].split(" ");
			if(words.length > 1){
				for(int i = 0;i < words.length;i++){
					float w = tvText.getTextWidthReal(words[i]);
					if((t_width + w) > extent.x){
						txt_temp += "\n" + words[i] + (i < words.length - 1 ? " ":"");
						t_width = w + (i < words.length - 1 ? tvText.getTextWidthReal(" ") : 0);
					}else{
						txt_temp += words[i] + (i < words.length - 1 ? " " : "");
						t_width += w + (i < words.length - 1 ? tvText.getTextWidthReal(" ") : 0);
					}
				}
			}else{
				for(byte i = 0;i < words[0].length();i++){
					String sampler = words[0].charAt(i)+"";
					float w = tvText.getTextWidthReal(sampler);
					if((t_width + w) > extent.x){
						txt_temp += "\n" + sampler + (i < words.length - 1 ? " ":"");
						t_width = w + (i < words.length - 1 ? tvText.getTextWidthReal(" ") : 0);
					}else{
						txt_temp += sampler + (i < words.length - 1 ? " " : "");
						t_width += w + (i < words.length - 1 ? tvText.getTextWidthReal(" ") : 0);
					}
				}
			}
			if(j < (lines.length - 1)){
				txt_temp += "\n";
				t_width = 0.0f;
			}
		}
		lines = txt_temp.split("\n");
		numLines = lines.length;
		if(numLines > 100){
			txt_temp = "";
			for(int i = (numLines - 100);i < lines.length;i++){
				txt_temp += (i > 1 ? "\n" : "") + lines[i];
			}
			lines = txt_temp.split("\n");
			numLines = lines.length;
		}
		int excedent = (numLines - max_lines);
		cursorY = (excedent > 0 ? excedent + 2 : 0f) * text_size * 2f;
		tvText.setText(txt_temp);
		cmd_txt = txt_temp;
	}
	
	byte state = -1;
	
	@Override
	public void onDraw(Drawer drawer) {
		if(print_queue.size() > 0){
			cmd_txt += print_queue.remove(0);
			state = 1;
		}
		if(state == 1 && print_queue.size() == 0){
			state = 2;
		}
		if(state == 2){
			updateListText(cmd_txt);
			state = -1;
		}
		drawer.setScale(extent.x,extent.y);
		drawer.scissorArea(local.x,local.y,extent.x,extent.y);
		drawer.renderQuad(local,background,-1);
		float dx = (local.x - extent.x);
		float dy = (local.y + extent.y) + cursorY;
		tvText.local.set(dx + tvText.getWidth(),dy - tvText.getHeight()) ;
		tvText.onDraw(drawer);
		if(!running_cmd || request_input){
			if(!request_input){
				etInput.setWidth(width - tvText.getCharWidth('>')*2f);
				etInput.updateExtent();
				etInput.local.set(dx + tvText.getCharWidth('>')*2f + etInput.getWidth(),tvText.local.y - tvText.getHeight() + text_size);
			}else{
				etInput.setWidth(width - tvText.getTextWidthReal(name) * 2f);
				etInput.updateExtent();
				etInput.local.set(dx + tvText.getTextWidthReal(name)*2f + etInput.getWidth(),tvText.local.y - tvText.getHeight() + text_size);
			}
			etInput.onDraw(drawer);
		}
		drawer.finishScissor();
	}
	
	public void printf(final String info,final int color){
		if(color == -1){
			print_queue.add("\n"+info);
		}else if(color == RED){
			print_queue.add("\n{|220,20,8:="+info+"|}");
		}else if(color == YELLOW){
			print_queue.add("\n{|230,230,1:="+info+"|}");
		}else if(color == GREEN){
			print_queue.add("\n{|8,220,8:="+info+"|}");
		}else if(color == ORANGE){
			print_queue.add("\n{|244,180,8:="+info+"|}");
		}else if(color == BLUE){
			print_queue.add("\n{|8,8,220:="+info+"|}");
		}
	}
	
	public void print(final String info,final int color){
		if(color == -1){
			print_queue.add(info);
		}else if(color == RED){
			print_queue.add("{|220,20,8:="+info+"|}");
		}else if(color == YELLOW){
			print_queue.add("{|230,230,1:="+info+"|}");
		}else if(color == GREEN){
			print_queue.add("{|8,220,8:="+info+"|}");
		}else if(color == ORANGE){
			print_queue.add("{|244,180,8:="+info+"|}");
		}else if(color == BLUE){
			print_queue.add("{|8,8,220:="+info+"|}");
		}
		
	}
	
	private void rewindInput(){
		printf(">",-1);
		etInput.setAutoFocus(true);
	}
	
	public void clear(){
		cmd_txt = ">";
		etInput.setAutoFocus(true);
	}

	@Override
	protected void notifyTouchOutside(float x, float y, byte type) {
		if(etInput.getKeyboard() != null && !etInput.getKeyboard().testTouch(x,y)){
			etInput.detachKeyBoard();
		}
	}
	
	public String processPath(String param) {
		String pth = param;
		for(String v : paths.keySet()){
			pth = pth.replace(v,paths.get(v));
		}
		return pth;
	}
	
	private void backPath(){
		for(int i = current_path.length()-1;i >= 0;i--){
			if(current_path.charAt(i) == '/'){
				current_path = current_path.substring(0,i);
				break;
			}
		}
	}
	
	@Override
	public void onChange(View view, String text, boolean deleting) {}

	@Override
	public void onEnter(View view, String text) {
		if(request_input){
			response = text;
			etInput.setText("");
			print(text,-1);
			request_input = false;
			return;
		}
		etInput.setText("");
		print(text,-1);
		int test = text.indexOf(" ");
		String program = test == -1 ? text : text.substring(0,test);
		final String[] args = test == -1 ? new String[0] : 
		text.substring(test + 1,text.length()).replace(" -","%").replace("-","").split("%");
		for(String cmd : commands.keySet()){
			if(program.equals(cmd)){
				final Command executor = commands.get(cmd);
				cmd_thread.addTask(new Task(){
						@Override
						public boolean execute() {
							run_cmd(executor,args);
							running_cmd = false;
							return true;
						}
				});
				running_cmd = true;
				return;
			}
		}
		printf("'"+program+"' command not exist.",RED);
		rewindInput();
	}
	
	private void run_cmd(Command cmd,String[] args){
		switch(cmd.exec(this,args)){
			case -2:
				return;
			case Command.CMD_ERROR:
				printf("Command execution error",RED);
				break;
			case Command.CMD_FINISHED:
				printf("Command execution OK",GREEN);
				break;
		}
		rewindInput();
	}
}
