package com.forcex.gtasdk;
import com.forcex.io.*;
import java.io.*;
import com.forcex.math.*;
import com.forcex.utils.*;
import com.forcex.collision.*;
import com.forcex.*;
import java.util.*;

public class DFFStream {
	private static final int CLUMP = 0x10;
	private static final int FRAMELIST = 0xE;
	private static final int GEOMETRYLIST = 0x1A;
	private static final int GEOMETRY = 0xF;
	private static final int MATERIALLIST = 0x8;
	private static final int MATERIAL = 0x7;
	private static final int FRAME = 0x253F2FE;
	private static final int EXTENSION = 0x3;
	private static final int BIN_MESH = 0x50E;
	private static final int SKIN_PLG = 0x116;
	private static final int VERTEX_NIGHT = 0x253F2F9;
	private static final int ATOMIC = 0x14;
	private static final int STRUCT = 0x01;
	private static final int HANIM = 0x11E;
	private static final int MATERIAL_EFFECT = 0x120;
	private static final int RENDER_TO_RIGHT = 0x1F;
	private static final int REFLECTION_MATERIAL = 0x253F2FC;
	private static final int SPECULAR_MAT = 0x253F2F6;
	private static final int COLLISION_MODEL = 0x253F2FA;
	private static final int ZMODELER_LOCK = 0xF21E;

	private static boolean readSection(MemoryStreamReader is, int sect)
	{
		int id = is.readInt();
		if (id == sect)
		{
			is.skip(8);
			return true;
		}
		else
		{
			is.skip(-4);
		}
		return false;
	}

	private static void skipSection(MemoryStreamReader is, int sect)
	{
		int id = is.readInt();
		if (id == sect)
		{
			is.skip(8);
		}
		else
		{
			is.skip(-4);
		}
	}
	
	private static String randomKey(){
		String name = "";
		for(byte i = 0;i < 10;i++){
			if(i % 2 == 0 && i > 1){
				name += (char)(97 + Math.random() * 25);
			}else{
				name += (char)(48 + Math.random() * 9);
			}
		}
		return name;
	}
	public static DFFSDK readDFF(String path, OnDFFStreamingListener listener, LanguageString lang)
	{
		boolean hasListener = (listener != null);
		DFFSDK dff = new DFFSDK();
		if (hasListener) {
			listener.onStreamPrint(lang.get("reading"));
			listener.onStreamProgress(4f);
		}
		MemoryStreamReader is = new MemoryStreamReader(path);
		String[] v = path.split("/");
		String name = v[v.length - 1];
		dff.name = name.substring(0,name.indexOf(".dff"));
		try {
			if(is.readInt() != CLUMP) {
				int lenght = is.readInt() + 4;
				is.skip(lenght);
			}else{
				is.seek(0);
			}
			if (readSection(is, CLUMP))
			{
				is.skip(-4);
				int version = is.readInt();
				dff.game = version;
				if (readSection(is, STRUCT))
				{
					dff.atomicCount = is.readInt();
					if (version > DFFGame.GTA3_1)
					{
						is.skip(8);
					}
				}
				if (readSection(is, FRAMELIST))
				{
					if (hasListener)
					{
						listener.onStreamPrint(lang.get("dffrf"));
						listener.onStreamProgress(6f);
					}
					is.skip(12); // struct
					dff.frameCount = is.readInt();
					for (int i = 0;i < dff.frameCount;i++)
					{
						if (hasListener)
						{
							listener.onStreamProgress(6f + (20f * ((float)i / dff.frameCount)));
						}
						DFFFrame frame = new DFFFrame();
						Matrix3f rot = new Matrix3f(is.readFloat(), is.readFloat(), is.readFloat(), is.readFloat(), is.readFloat(), is.readFloat(), is.readFloat(), is.readFloat(), is.readFloat());
						Vector3f pos = new Vector3f(is.readFloat(), is.readFloat(), is.readFloat());
						frame.rotation = rot;
						frame.position = pos;
						frame.parentIdx = is.readInt();
						frame.flags = is.readInt();
						dff.fms.add(frame);
					}
					for (int i = 0;i < dff.frameCount;i++)
					{
						skipSection(is, EXTENSION);
						if (is.readInt() == HANIM)
						{
							is.skip(8);
							DFFHanim hanim = new DFFHanim();
							hanim.unk1 = is.readInt();
							hanim.boneID = is.readInt();
							hanim.boneCount = is.readInt();
							if (hanim.boneCount > 0)
							{
								hanim.unk2 = is.readInt();
								hanim.unk3 = is.readInt();
							}
							for (int b = 0;b < hanim.boneCount;b++)
							{
								DFFHanim.Bone bone = new DFFHanim.Bone();
								bone.id = is.readInt();
								bone.num = is.readInt();
								bone.type = is.readInt();
								dff.bones.add(bone);
							}
							dff.fms.get(i).hanim = hanim;
						}
						else
						{
							is.skip(-4);
						}
						if (is.readInt() == FRAME) {
							int size = is.readInt();
							is.skip(4); 
							dff.fms.get(i).name = fixText(is.readString(size));
						}
						else
						{
							is.skip(-4);
						}
					}
					if (readSection(is, GEOMETRYLIST))
					{
						is.skip(12); // struct
						dff.geometryCount = is.readInt();
						for (int i = 0;i < dff.geometryCount;i++)
						{
							if (is.readInt() == GEOMETRY)
							{
								if (hasListener)
								{
									listener.onStreamPrint(lang.get("dffrg") + " " + (i + 1) + "/" + dff.geometryCount);
									listener.onStreamProgress(26f + (70f * ((float)(i + 1) / dff.geometryCount)));
								}
								is.skip(8);
								is.skip(12); // struct
								DFFGeometry geo = new DFFGeometry();
								geo.flags = is.readShort();
								geo.uvsets = is.readByte();
								boolean isMobile = is.readByte() == 1;
								if (isMobile)
								{
									listener.onStreamError(lang.get("dff_fail_mobile"), true);
									return null;
								}
								int triangleCount = is.readInt();
								geo.vertexCount = is.readInt();
								is.skip(4);
								if (version < DFFGame.GTAVC_2)
								{
									geo.ambient = is.readFloat();
									geo.diffuse = is.readFloat();
									geo.specular = is.readFloat();
								}
								if ((geo.flags & DFFGeometry.GEOMETRY_FLAG_COLORS) != 0)
								{
									geo.colors = is.readByteArray(geo.vertexCount * 4);
								}
								if ((geo.flags & DFFGeometry.GEOMETRY_FLAG_TEXCOORDS) != 0 || (geo.flags & DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS) != 0)
								{
									geo.texcoords = is.readFloatArray(geo.vertexCount * 2 * geo.uvsets);
								}
								if(geo.uvsets > 1 && (geo.flags & DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS) == 0){
									geo.flags |= DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS;
								}
								is.skip((triangleCount * 8) + (16));
								is.skip(8);
								geo.vertices = is.readFloatArray(geo.vertexCount * 3);
								if ((geo.flags & DFFGeometry.GEOMETRY_FLAG_NORMALS) != 0)
								{
									geo.normals  = is.readFloatArray(geo.vertexCount * 3);
								}
								if (readSection(is, MATERIALLIST))
								{
									is.skip(12); //struct
									int numMaterials = is.readInt();
									is.skip(numMaterials * 4);
									for (int m = 0;m < numMaterials;m++)
									{
										DFFMaterial mat = new DFFMaterial();
										if (readSection(is, MATERIAL))
										{
											is.skip(12);
											is.skip(4);
											mat.color = new Color(is.readUbyte(), is.readUbyte(), is.readUbyte(), is.readUbyte());
											is.skip(4);
											int texcount = is.readInt();
											mat.surfaceProp = is.readFloatArray(3);
											if (texcount > 0)
											{
												for(int t = 0;t < texcount;t++){
													if(t == 0){
														readTexture(mat, is);
													}else if(is.readInt() == 0x6){
														int sz = is.readInt();
														is.skip(sz + 4);
													}
												}
											}
											else
											{
												mat.texture = "";
											}
											if (is.readInt() == EXTENSION)
											{
												int size = is.readInt();
												if (size != 0)
												{
													is.skip(4);
													int offset = 0;
													while (offset < size)
													{
														int id = is.readInt();
														switch (id)
														{
															case RENDER_TO_RIGHT:
																{
																	is.skip(8);
																	mat.hasRenderToRight = true;
																	mat.RTRval1 = is.readInt();
																	mat.RTRval2 = is.readInt();
																	offset += 8 + 12;
																}
																break;
															case MATERIAL_EFFECT:
																{
																	int matfxSize = is.readInt();
																	is.skip(4);
																	mat.hasMaterialEffect = true;
																	mat.dataMatFx = is.readByteArray(matfxSize);
																	offset += (matfxSize + 12);
																}
																break;
															case REFLECTION_MATERIAL:
																{
																	int reflectSize = is.readInt();
																	is.skip(4);
																	mat.hasReflectionMat = true;
																	mat.reflectionAmount = is.readFloatArray(4);
																	mat.reflectionIntensity = is.readFloat();
																	is.skip(4);
																	offset += (reflectSize + 12);
																}
																break;
															case SPECULAR_MAT:
																{
																	int specularSize = is.readInt();
																	is.skip(4);
																	mat.hasSpecularMat = true;
																	mat.specular_level = is.readFloat();
																	mat.specular_name = is.readString(specularSize - 12);
																	is.skip(8);
																	offset += (specularSize + 12);
																}
																break;
															default: {
																	int tmp = is.readInt();
																	offset += tmp + 12;
																	is.skip(tmp + 4);
																}
																break;
														}
													}
												}
												else
												{
													is.skip(4);
												}
											}
											geo.materials.add(mat);
										}
									}
								}
								if (is.readInt() == EXTENSION)
								{
									final int size = is.getOffset() + is.readInt();
									is.skip(4);
									while (is.getOffset() < size)
									{
										switch (is.readInt())
										{
											case BIN_MESH:{
													is.skip(8);
													geo.isTriangleStrip = is.readInt() == 1;
													int splitCount = is.readInt();
													is.skip(4);
													for (int idx = 0;idx < splitCount;idx++)
													{
														DFFIndices index = new DFFIndices();
														int indexCount = is.readInt();
														index.material = is.readInt();
														index.index = new short[indexCount];
														for (int x = 0;x < indexCount;x++)
														{
															index.index[x] = (short)is.readInt();
														}
														geo.splits.add(index);
													}
												}
												break;
											case VERTEX_NIGHT:{
													int vert = is.readInt();
													is.skip(8);
													geo.nightColors = is.readByteArray(vert - 4);
												}
												break;
											case SKIN_PLG:{
													is.skip(8);
													DFFSkin skin = new DFFSkin();
													byte boneCount = is.readByte();
													int specialIndices = is.readByte();
													skin.unknowns = new byte[2];
													skin.unknowns[0] = is.readByte();
													skin.unknowns[1] = is.readByte();
													skin.specialBones = is.readByteArray(specialIndices);
													skin.boneIndices = is.readByteArray(geo.vertexCount * 4);
													skin.boneWeigts = is.readFloatArray(geo.vertexCount * 4);
													skin.boneMatrices = new Matrix4f[boneCount];
													for (byte b = 0;b < boneCount; b++){
														Matrix4f ibm = new Matrix4f(is.readFloatArray(16));
														skin.boneMatrices[b] = ibm;
													}
													if (specialIndices != 0) {
														is.skip(12);
													}
													geo.skin = skin;
												}
												break;
											case 0x253F2FD:{
													int tmp = is.readInt();
													is.skip(tmp + 4);
													geo.hasMeshExtension = true;
												}
												break;
											case 0x253F2F8:
												int s2dfx = is.readInt();
												is.skip(s2dfx);
												break;
											default:{
													int tmp = is.readInt();
													is.skip(tmp + 4);
												}
												break;
										}
									}
								}
								dff.geom.add(geo);
							}
						}
					}
					if (hasListener)
					{
						listener.onStreamPrint(lang.get("dffra"));
					}
					for (int i  = 0;i < dff.atomicCount; i++)
					{
						if (hasListener)
						{
							listener.onStreamPrint(lang.get("dffrao") + " " + (i + 1) + "/" + dff.atomicCount);
							listener.onStreamProgress(96f + (4f * ((float)(i + 1) / dff.atomicCount)));
						}
						if (readSection(is, ATOMIC))
						{
							is.skip(12);
							DFFAtomic atomic = new DFFAtomic();
							atomic.frameIdx = is.readInt();
							atomic.geoIdx = is.readInt();
							dff.fms.get(atomic.frameIdx).geoAttach = (short)atomic.geoIdx;
							dff.geom.get(atomic.geoIdx).frameIdx = atomic.frameIdx;
							dff.geom.get(atomic.geoIdx).name = dff.fms.get(atomic.frameIdx).name;
							atomic.unknow1 = is.readInt();
							is.skip(4);
							if (is.readInt() == EXTENSION)
							{
								final int size = is.getOffset() + is.readInt();
								is.skip(4);
								while (is.getOffset() < size)
								{
									int id = is.readInt();
									switch (id)
									{
										case RENDER_TO_RIGHT:
											{
												is.skip(8);
												atomic.hasRenderToRight = true;
												atomic.RTRval1 = is.readInt();
												atomic.RTRval2 = is.readInt();
											}
											break;
										case MATERIAL_EFFECT:
											{
												is.skip(8);
												atomic.hasMaterialEffect = true;
												atomic.materialFxType = is.readInt();
											}
											break;
										case ZMODELER_LOCK:
											if(listener != null){
												listener.onStreamError(lang.get("zm_lock",path),true);
											}
											return null;
										default:{
												int tmp = is.readInt();
												is.skip(tmp + 4);
											}
											break;
									}
								}
							}else{
								is.seek(-4);
							}
							dff.atomics.add(atomic);
						}
					}
				}
				if (is.readInt() == EXTENSION)
				{
					if (is.readInt() != 0)
					{
						is.skip(4);
						if (is.readInt() == COLLISION_MODEL)
						{
							int colSize = is.readInt();
							is.skip(4);
							dff.hasCollision = true;
							dff.col_id = randomKey();
							FileOutputStream col = new FileOutputStream(FX.homeDirectory + "zmdl/"+dff.col_id+".col");
							col.write(is.readByteArray(colSize));
							col.close();

						}
					}
				}
				if(!is.isEndOfFile()){
					if(is.readInt() == EXTENSION){
						dff.hasExtensionDff = true;
					}
				}
			}
			else
			{
				if (hasListener)
				{
					listener.onStreamError(lang.get("dff_header_error"), true);
				}
			}
			if (analizeDff(dff, listener, lang))
			{
				if (hasListener)
				{
					listener.onStreamPrint(lang.get("dff_finished"));
				}
				return dff;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
			if(hasListener){
				listener.onStreamError(lang.get("dff_exception") + "\n" + e.toString(), true);
			}
			Logger.log(e);
			dff = null;
			return null;
		}
		return null;
	}

	private static boolean analizeDff(DFFSDK dff, OnDFFStreamingListener listen, LanguageString lang)
	{
		if (listen != null)
		{
			listen.onStreamPrint(lang.get("analizing_dff"));
		}
		ArrayList<String> names_repeat = new ArrayList<>();
		for (short i = 0;i < dff.frameCount;i++)
		{
			String test = dff.getFrame(i).name;
			if (!dff.isSkin() && test.length() == 0)
			{
				if (listen != null)
				{
					listen.onStreamError(lang.get("no_name_frame"), false);
				}
				dff.fms.get(i).name = "no_name"+i;
				dff.errorNames = true;
			}
			for (short j = 0;j < dff.frameCount;j++)
			{
				if (j != i && dff.getFrame(j).name.equals(test))
				{
					if(names_repeat.indexOf(test) == -1){
						names_repeat.add(test);
					}
				}
			}
		}
		for(String test : names_repeat){
			if (listen != null) {
				listen.onStreamError("'" + test + "'" + lang.get("used_multiples_times"), false);
			}
		}
		names_repeat.clear();
		names_repeat = null;
		for(short i = 0;i < dff.geometryCount;i++){
			if(dff.geom.get(i).frameIdx == -1){
				if(listen != null){
					listen.onStreamError(lang.get("atomics_error"),true);
				}
				return false;
			}
		}
		return true;
	}

	private static void readTexture(DFFMaterial mat, MemoryStreamReader is)
	{
		is.skip(32); //Texture y Estructura
		int size = is.readInt();
		is.skip(4);
		mat.texture = fixText(cortarnombre(is.readString(size)));
		is.skip(4);
		int sizea = is.readInt();
		is.skip(16 + sizea);
	}
	
	
	public static boolean saveDFF(DFFSDK dff, String path, OnDFFStreamingListener listener, LanguageString lang)
	{
		try
		{
			RWSection clump = new RWSection(CLUMP, dff.game);
			RWSection struct = new RWSection(STRUCT, dff.game);
			if (listener != null)
			{
				listener.onStreamPrint(lang.get("exporting_dff"));
			}
			struct.writeInt(dff.atomicCount);
			if (dff.game > DFFGame.GTA3_1)
			{
				struct.writeInt(0);
				struct.writeInt(0);
			}
			clump.addSection(struct);
			RWSection framelist = new RWSection(FRAMELIST, dff.game);
			RWSection framestruct = new RWSection(STRUCT, dff.game);
			framestruct.writeInt(dff.frameCount);
			framestruct.addStorage(dff.frameCount * 56);
			if (listener != null)
			{
				listener.onStreamPrint(lang.get("exporting_framelist"));
			}
			for (int f = 0;f < dff.frameCount;f++)
			{
				framestruct.writeFloatArray(dff.fms.get(f).rotation.data);
				framestruct.writeVector(dff.fms.get(f).position);
				framestruct.writeInt(dff.fms.get(f).parentIdx);
				framestruct.writeInt(dff.fms.get(f).flags);
			}
			framelist.addSection(framestruct);
			for (int i = 0;i < dff.frameCount;i++)
			{
				RWSection frameExt = new RWSection(EXTENSION, dff.game);
				if (dff.fms.get(i).hanim != null)
				{
					RWSection hanim = new RWSection(HANIM, dff.game);
					DFFHanim ha = dff.fms.get(i).hanim; 
					hanim.writeInt(ha.unk1);
					hanim.writeInt(ha.boneID);
					hanim.writeInt(ha.boneCount);
					if (ha.boneCount != 0)
					{
						hanim.writeInt(ha.unk2);
						hanim.writeInt(ha.unk3);
					}
					for (int b = 0;b < ha.boneCount;b++)
					{
						DFFHanim.Bone bone = dff.bones.get(b);
						hanim.writeInt(bone.id);
						hanim.writeInt(bone.num);
						hanim.writeInt(bone.type);
					}
					frameExt.addSection(hanim);
				}
				if (dff.fms.get(i).name.length() > 0)
				{
					RWSection frame = new RWSection(FRAME, dff.game);
					frame.writeString(dff.fms.get(i).name.contains("no_name") ? "" :  dff.fms.get(i).name);
					frameExt.addSection(frame);
				}
				framelist.addSection(frameExt);
			}
			clump.addSection(framelist);
			RWSection geolist = new RWSection(GEOMETRYLIST, dff.game);
			writeGeometryList(dff, geolist, listener, lang);
			clump.addSection(geolist);
			writeAtomic(dff, clump, listener,lang);
			writeExtensionClump(dff, clump);
			if(dff.hasExtensionDff){
				clump.UpdateData();
				clump.writeInt(EXTENSION);
				clump.writeInt(0);
				clump.writeInt(dff.game);
			}
			byte result = clump.write(path,dff.hasExtensionDff);
			if (result != 0 && listener != null) {
				listener.onStreamError(result == -1 ? lang.get("memory_error") : lang.get("file_error"), true);
			}
			return result == 0;
		}
		catch (Exception e) {
			e.printStackTrace();
			Logger.log(e);
			if (listener != null) {
				listener.onStreamError(e.toString(), true);
			}
			return false;
		}
	}

	private static void writeGeometryList(DFFSDK dff, RWSection list, OnDFFStreamingListener listener, LanguageString lang)
	{
		RWSection geostruct = new RWSection(STRUCT, dff.game);
		geostruct.writeInt(dff.geometryCount);
		list.addSection(geostruct);
		for (int i = 0;i < dff.geometryCount;i++)
		{
			if (listener != null)
			{
				listener.onStreamPrint(lang.get("exporting_geometry") + " (" + i + ")");
				listener.onStreamProgress(((float)i / dff.geometryCount)*100f);
			}
			DFFGeometry geo = dff.geom.get(i);
			RWSection geometry = new RWSection(GEOMETRY, dff.game);
			RWSection struct = new RWSection(STRUCT, dff.game);
			struct.writeShort(geo.flags);
			struct.writeByte(geo.uvsets);
			struct.writeByte(0);
			short[] triangles = generateFaces(geo);
			int structSize = 12;
			structSize += 24;
			structSize += triangles.length * 2;
			structSize += geo.vertexCount * 3 * 4;
			structSize += geo.vertexCount * 2 * 4 * geo.uvsets;
			if (geo.colors != null)
			{
				structSize += geo.vertexCount * 4;
			}
			if (geo.hasNormals())
			{
				structSize += geo.vertexCount * 3 * 4;
			}
			struct.addStorage(structSize);
			struct.writeInt(triangles.length / 4);
			struct.writeInt(geo.vertexCount);
			struct.writeInt(1);
			if (dff.game < DFFGame.GTAVC_2)
			{
				struct.writeFloat(geo.ambient);
				struct.writeFloat(geo.diffuse);
				struct.writeFloat(geo.specular);
			}
			if (geo.colors != null)
			{
				struct.writeByteArray(geo.colors);
			}
			if (geo.texcoords != null)
			{
				struct.writeFloatArray(geo.texcoords);
			}
			struct.writeShortArray(triangles);
			BoundingBox box =  new BoundingBox();
			BoundingBox.create(box,geo.vertices);
			box.calculateExtents();
			BoundingSphere bs = box.toSphere();
			struct.writeFloat(bs.center.x);
			struct.writeFloat(bs.center.y);
			struct.writeFloat(bs.center.z);
			struct.writeFloat(bs.radius);
			struct.writeInt(1);
			struct.writeInt(geo.hasNormals() ? 1 : 0);
			struct.writeFloatArray(geo.vertices);
			if (geo.hasNormals())
			{
				struct.writeFloatArray(geo.normals);
			}
			geometry.addSection(struct);
			writeMaterialList(dff, geo, geometry);
			writeExtension(dff, geo, geometry, listener);
			list.addSection(geometry);
		}
	}

	private static void writeMaterialList(DFFSDK dff, DFFGeometry geo, RWSection geometry)
	{
		RWSection matList = new RWSection(MATERIALLIST, dff.game);
		RWSection struct = new RWSection(STRUCT, dff.game);
		struct.writeInt(geo.materials.size());
		struct.addStorage(geo.materials.size() * 4);
		for (int i = 0;i < geo.materials.size();i++)
		{
			struct.writeInt(-1);
		}
		matList.addSection(struct);
		for (int i = 0;i < geo.materials.size();i++)
		{
			writeMaterial(dff, geo.materials.get(i), matList);
		}
		geometry.addSection(matList);
	}

	private static void writeMaterial(DFFSDK dff, DFFMaterial mat, RWSection matlist)
	{
		RWSection material = new RWSection(MATERIAL, dff.game);
		RWSection struct = new RWSection(STRUCT, dff.game);
		struct.addStorage(28);
		struct.writeInt(0);
		struct.writeByteArray(mat.color.getData());
		struct.writeInt(0);
		struct.writeInt(mat.hasTexture() ? 1 : 0);
		struct.writeFloatArray(mat.surfaceProp);
		material.addSection(struct);
		if (mat.hasTexture()) {
			writeTextureMaterial(dff, mat, material);
		}
		RWSection extension = new RWSection(EXTENSION, dff.game);
		if (mat.hasRenderToRight) {
			extension.writeInt(RENDER_TO_RIGHT);
			extension.writeInt(8);
			extension.writeInt(dff.game);
			extension.writeInt(mat.RTRval1);
			extension.writeInt(mat.RTRval2);
		}
		if (mat.hasMaterialEffect) {
			extension.writeInt(MATERIAL_EFFECT);
			extension.writeInt(mat.dataMatFx.length);
			extension.writeInt(dff.game);
			extension.writeByteArray(mat.dataMatFx);
		}
		if (mat.hasReflectionMat) {
			extension.writeInt(REFLECTION_MATERIAL);
			extension.writeInt(24);
			extension.writeInt(dff.game);
			extension.writeFloatArray(mat.reflectionAmount);
			extension.writeFloat(mat.reflectionIntensity);
			extension.writeInt(0);
		}
		if (mat.hasSpecularMat) {
			extension.writeInt(SPECULAR_MAT);
			extension.writeInt(12 + mat.specular_name.length());
			extension.writeInt(0x1803FFFF);
			extension.writeFloat(mat.specular_level);
			extension.writeString(mat.specular_name);
			extension.writeInt(0);
			extension.writeInt(0);
		}
		material.addSection(extension);
		matlist.addSection(material);
	}

	private static void writeTextureMaterial(DFFSDK dff, DFFMaterial material, RWSection mat)
	{
		RWSection texture = new RWSection(0x06, dff.game);
		RWSection struct = new RWSection(STRUCT, dff.game);
		struct.writeInt(0x106);
		texture.addSection(struct);
		RWSection string1 = new RWSection(0x2, dff.game);
		RWSection string2 = new RWSection(0x2, dff.game);
		string1.writeString(material.texture + "\0");
		string2.writeInt(0);
		texture.addSection(string1);
		texture.addSection(string2);
		texture.addSection(new RWSection(EXTENSION, dff.game));
		mat.addSection(texture);
	}

	private static void writeExtension(DFFSDK dff, DFFGeometry geo, RWSection geometry, OnDFFStreamingListener listener)
	{
		RWSection extension = new RWSection(EXTENSION, dff.game);
		writeBinMesh(dff, geo, extension);
		if (geo.nightColors != null)
		{
			RWSection vertexNight = new RWSection(VERTEX_NIGHT, dff.game);
			vertexNight.writeInt(0);
			vertexNight.writeByteArray(geo.nightColors);
			extension.addSection(vertexNight);
		}

		if (geo.skin != null)
		{
			RWSection skinPlg = new RWSection(SKIN_PLG, dff.game);
			DFFSkin skin = geo.skin;
			skinPlg.writeByte(skin.boneMatrices.length);
			skinPlg.writeByte(skin.specialBones.length);
			skinPlg.writeByteArray(skin.unknowns);
			skinPlg.writeByteArray(skin.specialBones);
			skinPlg.writeByteArray(skin.boneIndices);
			skinPlg.writeFloatArray(skin.boneWeigts);
			for (int b = 0; b < skin.boneMatrices.length;b++)
			{
				if (dff.game != DFFGame.GTASA)
				{
					skinPlg.writeInt(0xdead);
				}
				skinPlg.writeFloatArray(skin.boneMatrices[b].data);
			}
			skinPlg.writeInt(0);
			skinPlg.writeInt(0);
			skinPlg.writeInt(0);
			extension.addSection(skinPlg);
		}
		if (geo.hasMeshExtension)
		{
			RWSection meshExt = new RWSection(0x253F2FD, dff.game);
			meshExt.writeInt(0);
			extension.addSection(meshExt);
		}
		geometry.addSection(extension);
	}
	private static void writeBinMesh(DFFSDK dff, DFFGeometry geo, RWSection extension)
	{
		RWSection binmesh = new RWSection(BIN_MESH, dff.game);
		binmesh.writeInt(geo.isTriangleStrip ? 1 : 0);
		binmesh.writeInt(geo.splits.size());
		int idxCount = 0;
		for (DFFIndices i : geo.splits)
		{
			idxCount += i.index.length;
		}
		binmesh.writeInt(idxCount);
		binmesh.addStorage(idxCount *  4 + (geo.splits.size() * 2 * 4));
		for (DFFIndices i : geo.splits)
		{
			idxCount = i.index.length;
			binmesh.writeInt(idxCount);
			binmesh.writeInt(i.material);
			for (int idx = 0;idx < idxCount;idx++)
			{
				binmesh.writeInt(i.index[idx]);
			}
		}
		extension.addSection(binmesh);
	}

	private static short[] generateFaces(DFFGeometry geo) {
		int size = 0;
		for (int j = 0;j < geo.splits.size();j++) {
			size += geo.splits.get(j).index.length / 3;
		}
		short[] faces = new short[size * 4];
		int tri = 0;
		int mat = 0;
		for (int j = 0;j < geo.splits.size();j++)
		{
			for (int i = 0;i < geo.splits.get(j).index.length - 2;i += 3)
			{
				faces[tri + 0] = geo.splits.get(j).index[i + 0];
				faces[tri + 1] = geo.splits.get(j).index[i + 2];
				faces[tri + 2] = (short)mat;
				faces[tri + 3] = geo.splits.get(j).index[i + 1];
				tri += 4;
			}
			mat++;
		}
		return faces;
	}

	private static void writeAtomic(DFFSDK dff, RWSection clump, OnDFFStreamingListener listener,LanguageString lang)
	{
		if (listener != null) {
			listener.onStreamPrint(lang.get("dff_finished"));
		}
		for (DFFAtomic a : dff.atomics) {
			RWSection atomic = new RWSection(ATOMIC, dff.game);
			RWSection struct = new RWSection(STRUCT, dff.game);
			struct.writeInt(a.frameIdx);
			struct.writeInt(a.geoIdx);
			struct.writeInt(a.unknow1);
			struct.writeInt(0);
			atomic.addSection(struct);
			RWSection extension = new RWSection(EXTENSION, dff.game);
			if (a.hasRenderToRight) {
				extension.writeInt(RENDER_TO_RIGHT);
				extension.writeInt(8);
				extension.writeInt(dff.game);
				extension.writeInt(a.RTRval1);
				extension.writeInt(a.RTRval2);
			}
			if (a.hasMaterialEffect) {
				extension.writeInt(MATERIAL_EFFECT);
				extension.writeInt(4);
				extension.writeInt(dff.game);
				extension.writeInt(a.materialFxType);
			}
			atomic.addSection(extension);
			clump.addSection(atomic);
		}
	}
	private static void writeExtensionClump(DFFSDK dff, RWSection clump)
	{
		RWSection extension = new RWSection(EXTENSION, dff.game);
		if (dff.hasCollision)
		{
			byte[] col = null;
			extension.writeInt(COLLISION_MODEL);
			try
			{
				FileInputStream is = new FileInputStream(FX.homeDirectory + "zmdl/"+dff.col_id+".col");
				extension.writeInt(is.available());
				col = new byte[is.available()];
				is.read(col);
				is.close();
			}
			catch (Exception e)
			{
				clump.addSection(extension);
				return;
			}
			extension.writeInt(dff.game);
			extension.writeByteArray(col);
		}
		clump.addSection(extension);
	}

	private static String cortarnombre(String str)
	{
        int indexOf = str.indexOf(0);
        return indexOf > 0 ? str.substring(0, indexOf) : str;
    }

	private static String fixText(String text)
	{
		String fixed = "";
		for (char c : text.toCharArray())
		{
			byte ch = (byte)c;
			if (ch >= 33 && ch <= 122 || ch == ' ')
			{
				fixed += c;
			}
			else
			{
				fixed += "x";
			}
		}
		return fixed;
	}
}
