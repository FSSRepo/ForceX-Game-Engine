package com.steward.prototype;

import com.forcex.app.*;
import com.forcex.gui.*;
import com.forcex.*;
import com.forcex.core.*;
import com.forcex.gui.widgets.*;
import com.forcex.math.*;
import com.forcex.utils.*;
import com.forcex.core.gpu.*;

public class PrototypeUNI extends Game implements InputListener {
	UIContext ctx;
	SlidePanel panel;
	EditText etA, etB, etC, etD, etE;
	TextView tvResult;
	ImageView ivTest;
	Button btnCalcular;
	Layout data_input,graph_layout;
	int ecuation_quat;
	int area_rect,area_cir,area_tri;
	int vol_cube,vol_parallele,vol_pyramid,vol_sphere;
	int func_linear,func_quadratic,func_exponential;
	Table values_table;
	PhysicsSimulation fisicas;
	boolean physics_simulation;
	Button btnMenuPhy;
	Layout menu_physics;
	ListView lv;
	TextView tvTimer;
	TextView tvCollisionInfo;

	@Override
	public void create() {
		/* 			
		
				Inicializar contexto de interfaz
				
			Tener en cuenta que la renderizacion 2D en Open GL esta definida
			desde -1 hasta 1 en los 2 ejes de coordenadas (X,Y)
			
		*/
		ctx = new UIContext();
		Layout main = new Layout(ctx);
		ctx.bindKeyBoard(0.7f); // crear un teclado
		/*
				Crear encabezado de la aplicacion
		*/
		ImageView header = new ImageView(-1,1f,0.15f);
		header.setMixColor(100,200,240);
		main.add(header);
		/*
			Bloques que contiene el encabezado
			* Header Container (Orientacion: Horizontal)
				 -> Logo Uni
				-> Text Container (Orientacion: Vertical)
					-> Nombre de la app
					-> Nombre de los integrantes
		*/

		Layout header_container = new Layout(ctx);
		header_container.setToWrapContent();
		header_container.setNoApplyConstraintY(true);
		header_container.setOrientation(Layout.HORIZONTAL);
		Layout text_header = new Layout(ctx);
		text_header.setToWrapContent();
		text_header.setMarginLeft(0.02f);
		
		// Imagen del logo de la UNI
		ImageView logoUni = new ImageView(Texture.load(FX.homeDirectory+"proto/logouni.png"),0.08f,0.06f);
		logoUni.setApplyAspectRatio(true);
		logoUni.setMarginLeft(0.01f);
		logoUni.setMarginTop(0.01f);
		
		// Texto: Nombre de la app
		TextView AppName = new TextView(UIContext.default_font);
		AppName.setText("Prototype documental research");
		AppName.setTextSize(0.06f);
		
		// Texto Integrantes
		TextView Integrantes = new TextView(UIContext.default_font);
		Integrantes.setText("Steward Garcia");
		Integrantes.setTextSize(0.05f);
		
		// Agregar a los contenedores en forma de hijos
		text_header.add(AppName);
		text_header.add(Integrantes);
		header_container.add(logoUni);
		header_container.add(text_header);
		main.add(header_container);
		/* 
		
			Lista de opciones
			
		*/
		ImageAdapter adp = new ImageAdapter(ctx);
		lv = new ListView(1f,0.85f,adp);
		lv.setBackgroundColor(230,230,230,128);
		lv.setRelativePosition(0,-0.3f);
		/*
			Cargar iconos de la lista
		*/
		int icon_quat = Texture.load(FX.homeDirectory+"proto/cuad.PNG");
		int icon_area_vol = Texture.load(FX.homeDirectory+"proto/geom.png");
		int icon_func = Texture.load(FX.homeDirectory+"proto/func.png");
		/*
			agregar items de la lista
		*/
		adp.add(new ImageItem(icon_quat,"Ecuaciones cuadraticas"));
		adp.add(new ImageItem(icon_area_vol,"Calcular area de un rectangulo"));
		adp.add(new ImageItem(icon_area_vol,"Calcular area de un triangulo"));
		adp.add(new ImageItem(icon_area_vol,"Calcular area de un circulo"));
		
		adp.add(new ImageItem(icon_area_vol,"Calcular volumen de un cubo"));
		adp.add(new ImageItem(icon_area_vol,"Calcular volumen de un paralelopipedo"));
		adp.add(new ImageItem(icon_area_vol,"Calcular volumen de una piramide"));
		adp.add(new ImageItem(icon_area_vol,"Calcular volumen de una esfera"));
		
		adp.add(new ImageItem(icon_func,"Graficar funcion lineal"));
		adp.add(new ImageItem(icon_func,"Graficar funcion cuadratica"));
		adp.add(new ImageItem(icon_func,"Graficar funcion exponencial"));
		
		adp.add(new ImageItem(Texture.load(FX.homeDirectory+"proto/fisica.png"),"Simulacion de fisicas"));
		/*
			Cargar las texturas (imagenes de referencia)
		*/
		ecuation_quat = Texture.load(FX.homeDirectory+"proto/cuadratica.png");
		area_rect = Texture.load(FX.homeDirectory+"proto/rectangulo.png");
		area_tri = Texture.load(FX.homeDirectory+"proto/triangulo.png");
		area_cir = Texture.load(FX.homeDirectory+"proto/circulo.png");
		vol_pyramid = Texture.load(FX.homeDirectory+"proto/piramide.png");
		vol_cube = Texture.load(FX.homeDirectory+"proto/cubo.png");
		vol_parallele = Texture.load(FX.homeDirectory+"proto/paralelopipedo.png");
		vol_sphere = Texture.load(FX.homeDirectory+"proto/esfera.png");
		func_linear = Texture.load(FX.homeDirectory+"proto/funcl.png");
		func_quadratic = Texture.load(FX.homeDirectory+"proto/funcc.png");
		func_exponential = Texture.load(FX.homeDirectory+"proto/funce.png");
		/*
			Cargar los eventos de la lista
		*/
		lv.setOnItemClickListener(new ListView.OnItemClickListener(){
				@Override
				public void onItemClick(ListView view, Object item, short position, boolean longclick) {
					/*
						los items en una lista empiezan a contarse desde 0
					*/
					if(position == 0){
						resolverEcuacionesCuadraticas();
					}else if(position > 0 && position < 4){
						resolverAreas(position - 1);
					}else if(position >= 4 && position <= 7){
						resolverVolumenes(position - 4);
					}else if(position >= 8 && position <= 10){
						graficarFunciones(position - 8);
					}else if(position == 11){
						lv.setVisibility(View.GONE);
						btnMenuPhy.setVisibility(View.VISIBLE);
						tvCollisionInfo.setVisibility(View.VISIBLE);
						physics_simulation = true;
						fisicas.reset();
					}
					panel.setBackGoundColor(0xffffffff);
				}
		});
		main.add(lv);
		btnMenuPhy = new Button("Menu",UIContext.default_font,0.06f,0.05f);
		btnMenuPhy.setMarginTop(0.02f);
		btnMenuPhy.setMarginRight(0.02f);
		btnMenuPhy.setAlignment(Layout.RIGHT);
		btnMenuPhy.setVisibility(View.GONE);
		btnMenuPhy.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view)
				{
					if(fisicas.show_menu){
						closeMenuPhy();
					}else{
						openMenuPhy();
					}
				}
		});
		main.add(btnMenuPhy);
		tvCollisionInfo = new TextView(UIContext.default_font);
		tvCollisionInfo.setText("No hay colisiones");
		tvCollisionInfo.setTextSize(0.04f);
		tvCollisionInfo.setMarginTop(0.02f);
		tvCollisionInfo.setTextColor(20,230,20);
		tvCollisionInfo.setNoApplyConstraintY(true);
		tvCollisionInfo.setVisibility(View.GONE);
		main.add(tvCollisionInfo);
		/*
			Cargar las otras vistas
		*/
		ctx.setContentView(main);
		prepareCalculator();
		prepareGraphicator();
		prepareMenu();
		fisicas = new PhysicsSimulation(this);
		fisicas.create();
		FX.device.addInputListener(this);
	}
	
	private void openMenuPhy(){
		btnMenuPhy.setText("Cerrar");
		fisicas.show_menu = true;
		panel.setBackGoundColor(0x0);
		tvCollisionInfo.setVisibility(View.GONE);
		panel.showWithContent(menu_physics);
	}
	
	private void closeMenuPhy(){
		btnMenuPhy.setText("Menu");
		fisicas.show_menu = false;
		panel.dimiss();
		tvCollisionInfo.setVisibility(View.VISIBLE);
	}
	
	private void prepareMenu(){
		menu_physics = new Layout(ctx);
		menu_physics.setToWrapContent();
		menu_physics.setUseWidthCustom(true);
		menu_physics.setWidth(0.5f);
		TextView tvSim = new TextView(UIContext.default_font);
		tvSim.setText("Simulation is running...");
		tvSim.setTextSize(0.08f);
		tvSim.setMarginTop(0.03f);
		tvSim.setTextColor(255,255,255);
		tvSim.setMarginLeft(0.01f);
		menu_physics.add(tvSim);
		ProgressBar bar = new ProgressBar(0.5f,0.02f);
		bar.setColor(0x0,0xffCC0ACC);
		bar.setProgress(40);
		bar.setIndeterminate(true);
		bar.setUseEdge(false);
		menu_physics.add(bar);
		tvTimer = new TextView(UIContext.default_font);
		tvTimer.setText("00:00");
		tvTimer.setTextSize(0.04f);
		tvTimer.setMarginTop(0.02f);
		tvTimer.setTextColor(255,255,255);
		tvTimer.setAlignment(Layout.RIGHT);
		menu_physics.add(tvTimer);
		Button btnMenuReset = new Button("Reiniciar",UIContext.default_font,0.13f,0.07f);
		btnMenuReset.setMarginTop(0.03f);
		btnMenuReset.setAlignment(Layout.CENTER);
		btnMenuReset.setBackgroundColor(80,10,80);
		btnMenuReset.setTextColor(255,255,255);
		btnMenuReset.setTextSize(0.06f);
		btnMenuReset.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					fisicas.reset();
					closeMenuPhy();
				}
			});
		menu_physics.add(btnMenuReset);
		Button btnAdd = new Button("Agregar Mas",UIContext.default_font,0.13f,0.07f);
		btnAdd.setMarginTop(0.03f);
		btnAdd.setAlignment(Layout.CENTER);
		btnAdd.setBackgroundColor(80,10,80);
		btnAdd.setTextColor(255,255,255);
		btnAdd.setTextSize(0.06f);
		btnAdd.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					fisicas.addMore();
					closeMenuPhy();
				}
			});
		menu_physics.add(btnAdd);
		Button btnCerrarPhy = new Button("Salir",UIContext.default_font,0.13f,0.07f);
		btnCerrarPhy.setMarginTop(0.02f);
		btnCerrarPhy.setAlignment(Layout.CENTER);
		btnCerrarPhy.setBackgroundColor(80,10,80);
		btnCerrarPhy.setTextColor(255,255,255);
		btnCerrarPhy.setTextSize(0.06f);
		btnCerrarPhy.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					lv.setVisibility(View.VISIBLE);
					closeMenuPhy();
					physics_simulation = false;
					btnMenuPhy.setVisibility(View.GONE);
					tvCollisionInfo.setVisibility(View.GONE);
				}
			});
		menu_physics.add(btnCerrarPhy);
	}

	@Override
	public int pause(int type) {
		if(type == EventType.BACK_BUTTON){
			return EventType.REQUEST_EXIT;
		}
		return 0;
	}
	
	/*
		Preparar y mostrar la vista en modo calculadora
		de ecuaciones cuadraticas
	*/
	private void resolverEcuacionesCuadraticas(){
		panel.showWithContent(data_input);
		setEditTextVisible(3);
		setEditTextHint("a","b","c");
		tvResult.setText("");
		etA.setAutoFocus(true);
		ivTest.setTexture(ecuation_quat);
		btnCalcular.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					try{
						/*
						 Validar los valores introducidos
						 */
						float a = 1f,b = 0.0f,c = 0.0f;
						if(!etA.isEmpty()){
							a = etA.getNumber();
						}else{
							Toast.info("El valor de 'a' por defecto es 1",2f);
						}
						if(etB.isEmpty()){
							Toast.error("Campo 'b' requerido!!",2f);
							return;
						}else{
							b = etB.getNumber();
						}
						if(etC.isEmpty()){
							Toast.error("Campo 'c' requerido!!",2f);
							return;
						}else{
							c = etC.getNumber();
						}
						tvResult.setText("Resultados:\n"+solveEcuationQuadratic(a,b,c));
					}catch(Exception e){
						Toast.error("Error: "+e.toString(),2f);
					}
				}
			});
	}
	/*
	 Preparar y mostrar la vista en modo calculadora
	 de areas.
	 El parametro offset se utiliza para poder utilizar
	 los distintos modos Area de un rectangulo, Area de un triangulo, etc.
	 */
	private void resolverAreas(final int offset){
		panel.showWithContent(data_input);
		etA.setAutoFocus(true);
		switch(offset){
			case 0: // rectangulo
				setEditTextVisible(2);
				setEditTextHint("ancho","altura");
				ivTest.setTexture(area_rect);
				break;
			case 1: // triangulo
				setEditTextVisible(2);
				setEditTextHint("base","altura");
				ivTest.setTexture(area_tri);
				break;
			case 2: // circulo
				setEditTextVisible(1);
				setEditTextHint("radio");
				ivTest.setTexture(area_cir);
				break;
		}
		tvResult.setText("");
		btnCalcular.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					try{
						/*
						 Validar los valores introducidos
						 */
						float a = 1f,b = 0.0f;
						if(etA.isEmpty()){
							Toast.error("Campo '"+etA.getHint()+"' requerido!!",2f);
							return;
						}else{
							a = etA.getNumber();
						}
						if(offset != 2){
							if(etB.isEmpty()){
								Toast.error("Campo '"+etB.getHint()+"' requerido!!",2f);
								return;
							}else{
								b = etB.getNumber();
							}
						}
						switch(offset){
							case 0:
								tvResult.setText("Area del rectangulo: "+solveRectangleArea(a,b)+" u²");
								break;
							case 1:
								tvResult.setText("Area del triangulo: "+solveTriangleArea(a,b)+" u²");
								break;
							case 2:
								tvResult.setText("Area del circulo: "+solveCircleArea(a)+" u²");
								break;
						}
					}catch(Exception e){
						Toast.error("Error: "+e.toString(),2f);
					}
				}
			});
	}
	/*
	 Preparar y mostrar la vista en modo calculadora
	 de volumenes.
	 El parametro offset se utiliza para poder utilizar
	 los distintos modos Volumen de un Cubo, Volumen de un paralelopipedo, etc.
	 */
	private void resolverVolumenes(final int offset){
		panel.showWithContent(data_input);
		etA.setAutoFocus(true);
		switch(offset){
			case 0: // cubo
				setEditTextVisible(1);
				setEditTextHint("lado");
				ivTest.setTexture(vol_cube);
				break;
			case 1: // paralelopipedo
				setEditTextVisible(3);
				setEditTextHint("ancho","altura","profund.");
				ivTest.setTexture(vol_parallele);
				break;
			case 2: // piramide
				setEditTextVisible(3);
				setEditTextHint("ancho","altura","profund.");
				ivTest.setTexture(vol_pyramid);
				break;
			case 3: // esfera
				setEditTextVisible(1);
				setEditTextHint("radio");
				ivTest.setTexture(vol_sphere);
				break;
		}
		tvResult.setText("");
		btnCalcular.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					try{
						float a = 0f,b = 0f,c =0f;
						/*
							Validar los valores introducidos
						*/
						if(etA.isEmpty()){
							Toast.error("Campo '"+etA.getHint()+"' requerido!!",2f);
							return;
						}else{
							a = etA.getNumber();
						}
						if(offset != 0 && offset != 3){
							if(etB.isEmpty()){
								Toast.error("Campo '"+etB.getHint()+"' requerido!!",2f);
								return;
							}else{
								b = etB.getNumber();
							}
							if(etC.isEmpty()){
								Toast.error("Campo '"+etC.getHint()+"' requerido!!",2f);
								return;
							}else{
								c = etC.getNumber();
							}
						}
						switch(offset){
							case 0:
								tvResult.setText("Volumen del cubo: "+solveCubeVolume(a)+" u³");
								break;
							case 1:
								tvResult.setText("Volumen del paralelopipedo: "+solveParallelepipedVolume(a,b,c)+" u³");
								break;
							case 2:
								tvResult.setText("Volumen de la piramide: "+solvePyramidVolume(a,b,c)+" u³");
								break;
							case 3:
								tvResult.setText("Volumen de la esfera: "+solveSphereVolume(a)+" u³");
								break;
						}
					}catch(Exception e){
						Toast.error("Error: "+e.toString(),2f);
					}
				}
			});
	}
	
	/*
	 Preparar y mostrar la vista en modo graficar funciones.
	 El parametro offset se utiliza para poder utilizar
	 los distintos modos funcion lineal,funcion cuadratica,funcion exponencial.
	 */
	 
	float func_a,func_b,func_c;
	private void graficarFunciones(final int offset){
		panel.showWithContent(data_input);
		etA.setAutoFocus(true);
		switch(offset){
			case 0: // funcion lineal
				setEditTextVisible(3);
				setEditTextHint("a","b","intervalo");
				ivTest.setTexture(func_linear);
				break;
			case 1: // funcion cuadratica
				setEditTextVisible(4);
				setEditTextHint("a","b","c","intervalo");
				ivTest.setTexture(func_quadratic);
				break;
			case 2: // funcion exponencial
				setEditTextVisible(2);
				setEditTextHint("n","intervalo");
				ivTest.setTexture(func_exponential);
				break;
		}
		tvResult.setText("");
		btnCalcular.setText("Graficar");
		btnCalcular.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					try{
						float start = 0,end = 0;
						/*
							Validar los valores introducidos
						*/
						if(etA.isEmpty()){
							Toast.error("Campo '"+etA.getHint()+"' requerido!!",2f);
							return;
						}else{
							func_a = etA.getNumber();
						}
						if(etB.isEmpty()){
							Toast.error("Campo '"+etB.getHint()+"' requerido!!",2f);
							return;
						}else{
							if(offset != 2){
								func_b = etB.getNumber();
							}else{
								String[] spl = etB.getText().split("--");
								start = Float.parseFloat(spl[0]);
								end = Float.parseFloat(spl[1]);
							}
						}
						if(offset == 0 || offset == 1){
							if(etC.isEmpty()){
								Toast.error("Campo '"+etC.getHint()+"' requerido!!",2f);
								return;
							}else{
								if(offset != 0){
									func_c = etC.getNumber();
								}else{
									String[] spl = etC.getText().split("--");
									start = Float.parseFloat(spl[0]);
									end = Float.parseFloat(spl[1]);
								}
							}
						}
						if(offset == 1){
							if(etD.isEmpty()){
								Toast.error("Campo '"+etD.getHint()+"' requerido!!",2f);
								return;
							}else{
								String[] spl = etD.getText().split("--");
								start = Float.parseFloat(spl[0]);
								end = Float.parseFloat(spl[1]);
							}
						}
						FunctionGraph.Function func = null;
						switch(offset){
							case 0:
								func = new FunctionGraph.Function(){
									@Override
									public float f(float x) {
										/*
											Funcion lineal
										*/
										return func_a * x + func_b;
									}
								};
								break;
							case 1:
								func = new FunctionGraph.Function(){
									@Override
									public float f(float x) {
										/*
										 Funcion cuadratica
										 */
										return func_a * (x*x) + func_b * x + func_c;
									}
								};
								break;
							case 2:
								func = new FunctionGraph.Function(){
									@Override
									public float f(float x) {
										/*
										 Funcion exponencial
										 */
										return Maths.pow(x,func_a);
									}
								};
								break;
						}
						FunctionGraph graph = new FunctionGraph(0.7f,0.7f,func,start,end,1.0f / 100f);
						graph.integrate();
						graph.setId(0x895);
						graph_layout.add(graph,0);
						float rate = Maths.abs(start - end) / 10.0f;
						String[][] table = new String[12][];
						table[0] = new String[]{"X","Y"};
						float offx = start;
						for(int i = 1;i <= 11;i++){
							float y = func.f(offx);
							table[i] = new String[]{
									String.format("%.3f",offx), // X
									y < 1000 && y > -1000 ? String.format("%.3f",y) :
									y < 0 ? "y < -1000":"y > 1000"};
							offx += rate;
						}
						values_table.setContent(table);
						panel.showWithContent(graph_layout);
					}catch(Exception e){
						Toast.error("Error: "+e.toString(),2f);
					}
				}
			});
	}
	
	/*
		 Inicializar los componentes para graficar una funcion
	*/
	
	private void prepareGraphicator(){
		graph_layout = new Layout(ctx);
		graph_layout.setToWrapContent();
		graph_layout.setUseWidthCustom(true);
		graph_layout.setOrientation(Layout.HORIZONTAL);
		graph_layout.setWidth(1f);
		Layout container = new Layout(ctx);
		container.setToWrapContent();
		Button close = new Button("Cerrar",UIContext.default_font,0.18f,0.05f);
		close.setApplyAspectRatio(true);
		close.setTextSize(0.06f);
		close.setMarginTop(0.02f);
		close.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					panel.dimiss();
					FunctionGraph v = (FunctionGraph)ctx.findViewByID(0x895);
					if(v != null){
						graph_layout.remove(v);
						v.destroy();
					}
				}
			});
		container.add(close);
		values_table = new Table(0.28f,0.7f);
		values_table.setMarginTop(0.02f);
		values_table.setContent(new String[][]{{"X","Y"}});
		container.add(values_table);
		graph_layout.add(container);
	}
	
	/*
		 Inicializar los componentes para
		 la calculadora de ecuacion,area,volumen.
	 */
	private void prepareCalculator() {
		data_input = new Layout(ctx);
		data_input.setToWrapContent();
		data_input.setUseWidthCustom(true);
		data_input.setWidth(1f);
		Layout content = new Layout(ctx);
		content.setToWrapContent();
		content.setOrientation(Layout.HORIZONTAL);
		ivTest = new ImageView(-1,0.5f,0.28f);
		ivTest.setApplyAspectRatio(true);
		ivTest.setAlignment(Layout.CENTER);
		content.add(ivTest);
		Layout container = new Layout(ctx);
		container.setToWrapContent();
		TextView tvInput = new TextView(UIContext.default_font);
		tvInput.setTextSize(0.05f);
		tvInput.setText("Entrada");
		tvInput.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
		container.add(tvInput);
		etA = new EditText(ctx,0.15f,0.06f,0.06f);
		etB = new EditText(ctx,0.15f,0.06f,0.06f);
		etC = new EditText(ctx,0.15f,0.06f,0.06f);
		etD = new EditText(ctx,0.15f,0.06f,0.06f);
		etE = new EditText(ctx,0.15f,0.06f,0.06f);
		etA.setMarginTop(0.02f); etA.setId(0x21);
		etB.setMarginTop(0.02f); etB.setId(0x22);
		etC.setMarginTop(0.02f); etC.setId(0x23);
		etD.setMarginTop(0.02f); etD.setId(0x24);
		etE.setMarginTop(0.02f); etE.setId(0x25);
		
		container.add(etA);
		container.add(etB);
		container.add(etC);
		container.add(etD);
		container.add(etE);
		
		container.setMarginLeft(0.05f);
		content.add(container);
		
		tvResult = new TextView(UIContext.default_font);
		tvResult.setTextSize(0.055f);
		tvResult.setMarginLeft(0.04f);
		tvResult.setMarginTop(0.04f);
		tvResult.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
		data_input.add(content);
		data_input.add(tvResult);
		Layout btn_container = new Layout(ctx);
		btn_container.setToWrapContent();
		btn_container.setMarginLeft(0.1f);
		btn_container.setMarginTop(0.1f);
		btnCalcular = new Button("Calcular",UIContext.default_font,0.15f,0.07f);
		btnCalcular.setApplyAspectRatio(true);
		btnCalcular.setTextSize(0.06f);
		btn_container.add(btnCalcular);
		Button close = new Button("Cerrar",UIContext.default_font,0.15f,0.07f);
		close.setApplyAspectRatio(true);
		close.setTextSize(0.06f);
		close.setMarginTop(0.02f);
		close.setAlignment(Layout.CENTER);
		close.setOnClickListener(new View.OnClickListener(){
				@Override
				public void OnClick(View view) {
					panel.dimiss();
					KeyBoard.instance.forceClose();
				}
			});
		btn_container.add(close);
		content.add(btn_container);
		panel = new SlidePanel(data_input);
		panel.setHeight(0.85f);
		panel.setYPosition(-0.15f);
		panel.setSpecialDimiss(true);
		ctx.setSlidePanel(panel);
	}
	
	private void setEditTextVisible(int end){
		for(int i = 1;i <= 5;i++){
			ctx.findViewByID(0x20+i).setVisibility(i <= end ? View.VISIBLE : View.GONE);
			((EditText)ctx.findViewByID(0x20+i)).setText("");
			((EditText)ctx.findViewByID(0x20+i)).setNumbersMode(true);
		}
	}
	
	private void setEditTextHint(String... hints){
		for(int i = 0;i < 5;i++){
			((EditText)ctx.findViewByID(0x21+i)).setHint(i < hints.length ? hints[i] :"");
		}
	}
	
	/*
		Funciones de calculo
	*/
	private String solveCubeVolume(float lado){
		// volumen de un cubo
		float volumen = Maths.pow(lado,3);
		return String.format("%.3f",volumen);
	}
	
	private String solveParallelepipedVolume(float width,float height,float depth){
		// volumen de un paralelopipedo
		float volumen = width * height * depth;
		return String.format("%.3f",volumen);
	}

	private String solvePyramidVolume(float width,float height,float depth){
		// volumen de una piramide
		float area_base = width * depth;
		float volumen = (area_base * height) / 3.0f;
		return String.format("%.3f",volumen);
	}

	private String solveSphereVolume(float radio){
		// volumen de una esfera
		float volumen = (4/3) * Maths.PI * (radio*radio*radio);
		return String.format("%.3f",volumen);
	}
	
	
	private String solveRectangleArea(float width,float height){
		// area de un rectangulo
		float area = width * height;
		return String.format("%.3f",area);
	}
	
	private String solveTriangleArea(float width,float height){
		// area de un triangulo
		float area = (width*height) / 2.0f;
		return String.format("%.3f",area);
	}
	
	private String solveCircleArea(float radio){
		// area de un circulo
		float area = Maths.PI * (radio*radio);
		return String.format("%.3f",area);
	}
	
	private String solveEcuationQuadratic(float a,float b,float c){
		// Aplicando la formula general para resolver las ecuaciones cuadraticas
		String result = "";
		// Determinante
		float det = b*b - 4*a*c;
		if(det == 0.0f){
			result += "X = "+String.format("%.3f",-b / (2f * a));
		}else if(det < 0.0f){
			result += "No hay solucion";
		}else{
			float x1 = (-b + Maths.sqrt(det)) / 2f*a;
			float x2 = (-b - Maths.sqrt(det)) / 2f*a;
			result += "X1 = "+String.format("%.3f",x1)+"\n";
			result += "X2 = "+String.format("%.3f",x2);
		}
		return result;
	}
	/*
		Metodo de renderizacion en OpenGL
	*/
	@Override
	public void render(float deltaTime) {
		if(physics_simulation){
			fisicas.render(deltaTime,ctx);
		}else{
			FX.gl.glClearColor(0.8f,0.8f,0.8f,1);
			FX.gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
			ctx.draw();
		}
	}
	
	float ox, oy;
	boolean first = true;
	
	@Override
	public void onTouch(float x, float y, byte type, byte pointer){
		Vector2f touch = GameUtils.getTouchNormalized(x,y);
		if(!ctx.testTouch(touch.x,touch.y)){
			if(!fisicas.show_menu){
				if(type == EventType.TOUCH_DRAGGING){
					float nx = (x - ox) * 0.1f;
					float ny = (y - oy) * 0.1f;
					fisicas.camera.orbit(ny,nx);
				} else if(first) {
					first = false;
					ox = x;
					oy = y;
				}
				ox = x;
				oy = y;
			}
		} else {
			ctx.onTouch(touch.x, touch.y, type);
		}
	}

	@Override
	public void onKeyEvent(byte key, boolean down)
	{
		ctx.onKeyEvent(key, down);
	}

	@Override
	public void destroy() {
		ctx.destroy();
		fisicas.destroy();
	}

	/*
		Adaptador de lista
	*/
	static class ImageAdapter extends ListAdapter<ImageItem> {
		ImageView iv;
		TextView tv;
		
		public ImageAdapter(UIContext ctx){
			super(ctx);
		}
		
		@Override
		protected void createView(Layout container) {
			iv = new ImageView(-1,0.065f,0.065f);
			iv.setApplyAspectRatio(true);
			iv.setMarginTop(0.02f);
			iv.setMarginBottom(0.02f);
			getContext();
			tv = new TextView(UIContext.default_font);
			tv.setMarginTop(0.05f);
			tv.setMarginLeft(0.04f);
			tv.setTextSize(0.06f);
			container.setOrientation(Layout.HORIZONTAL);
			container.add(iv);
			container.add(tv);
		}

		@Override
		protected void updateView(ImageItem item, short position, Layout container) {
			iv.setTexture(item.icon);
			tv.setText(item.text);
		}
	}

	static class ImageItem {
		public int icon;
		public String text;
		
		public ImageItem(int icon,String text){
			this.icon = icon;
			this.text = text;
		}
	}

}