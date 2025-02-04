package com.forcex.gtasdk;

public class DFFAtomic
{
	public int frameIdx;
	public int geoIdx;
	
	// no essential
	public int unknown1;
	public boolean hasRenderToRight;
	public int RTRval1;
	public int RTRval2;
	public boolean hasMaterialEffect;
	public int materialFxType;

	@Override
	public DFFAtomic clone() {
		DFFAtomic atm = new DFFAtomic();
		atm.RTRval1 = RTRval1;
		atm.RTRval2 = RTRval2;
		atm.materialFxType = materialFxType;
		atm.hasRenderToRight = hasRenderToRight;
		atm.hasMaterialEffect = hasMaterialEffect;
		atm.unknown1 = unknown1;
		return atm;
	}
}
