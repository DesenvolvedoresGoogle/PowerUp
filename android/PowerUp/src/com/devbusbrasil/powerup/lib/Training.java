package com.devbusbrasil.powerup.lib;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class Training {
	
	private static Training mInstance;
	public static Training GetInstance() {
		if (mInstance == null) {
			mInstance = new Training();
		}
		return mInstance;
	}
	
	private int mId;
	private List<TrainingItem> mTrainingList;
	
	public int getId() {
		return mId;
	}
	public void setId(int pId) {
		this.mId = pId;
	}
	public List<TrainingItem> getTrainingList() {
		return mTrainingList;
	}
	public void setTrainingList(List<TrainingItem> pTrainingList) {
		this.mTrainingList = pTrainingList;
	}
	
	public Training() {
		this.mTrainingList = new ArrayList<TrainingItem>();
	}
	
	public void addExercise(TrainingItem pTrainingItem) {
		this.mTrainingList.add(pTrainingItem);
	}
	
	public TrainingItem findTrainingItem(String pEquipment) {
		for (int i = 0; i < mTrainingList.size(); i++) {
			Log.w("POWERUP", i + " " + mTrainingList.get(i).getEquipment() + " " + pEquipment);
			//if (mTrainingList.get(i).getEquipment().trim() == pEquipment.trim()) {
			if (pEquipment.trim().equals(mTrainingList.get(i).getEquipment().trim())) {
				Log.w("POWERUP", "HAHAHAHAHAHAHAHAHAHA");
				return mTrainingList.get(i);
			}
		}
		return null;
	}
}