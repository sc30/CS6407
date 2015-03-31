package soh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import soc.ReportObservable;

public class SOHSystem {
	 
	private ArrayList<SOHObserver> observers = new ArrayList<SOHObserver>();
	
	float voltage[];
	float Temperature;
	float Current;
	int time;
	static BMS bms ;
	final static String PRESENTCAPACITY = "PRESENTCAPACITY";
	final static String INITIALCAPACITY = "INITIALCAPACITY";
	float PresentCapacity_me;
	float InCA_me;
	CPUOut cpuOut;
	SOHSystem s;

	private int stateOfBattery;
	
	public int getStateOfBattery() {
		return stateOfBattery;
	}

	public void setStateOfBattery(int stateOfBattery) {
		this.stateOfBattery = stateOfBattery;
		notifyAllObservers();
	}

	public void addObserver(SOHObserver observer) {
	        observers.add(observer);
	    }

	    public void notifyAllObservers() {
	        for (SOHObserver observer : observers) {
	            observer.update();
	        }
	    }

	public SOHSystem(float[] voltage, float Temperature, float Current, int time) {
		// TODO Auto-generated constructor stub
		this.voltage = voltage;
		this.Current = Current;
		this.Temperature = Temperature;
		this.time = time;

	}

	BatteryOut getDataFromBattery() {
		BatteryOut batteryOut = new BatteryOut(voltage, Temperature, Current);

		return batteryOut;
	}

	int getBrokenCell() {
		int broken = 0;
		for (int i = 0; i < voltage.length; i++) {
			if  (voltage[i]<=0 || voltage[i] > 120) {
				broken = i + 1;
				break;
			}
		}

		return broken;

	}

	SensorOut getDataFromSensor(BatteryOut batteryOut) {
		float[] v_BO = batteryOut.getVoltage();
		float T_BO = batteryOut.getTemperature();
		float c_BO = batteryOut.getCurrent();

		SensorOut sensorOut = new SensorOut(v_BO, T_BO, c_BO);
		return sensorOut;

	}

	@SuppressWarnings({ "unused", "static-access" })
	void storeDataToMemory(SensorOut sensorOut) {

		float v_SO = sensorOut.getVoltage();
		float T_SO = sensorOut.getTemperature();
		float c_SO = sensorOut.getCurrent();

		MemoryOut memoryOut = new MemoryOut(v_SO, time, c_SO);
		// HashMap<String,float> hashMap = new HashMap<String,float>();

		float PresentCapacity = memoryOut.getPresentCapacity();
		float InCA = memoryOut.getInitialCapacity();
//		float PresentCapacity = 15000;
//		float InCA = 18000;
		bms.centralStorage.put(PRESENTCAPACITY, PresentCapacity);
		bms.centralStorage.put(INITIALCAPACITY, InCA);
	}

	@SuppressWarnings("static-access")
	void getDataFromCPU() {

		 PresentCapacity_me = (float) bms.centralStorage
				.get(PRESENTCAPACITY);
		 InCA_me = (float) bms.centralStorage.get(INITIALCAPACITY);

	}

	@SuppressWarnings("static-access")
	int getRUL() {
		bms= new BMS();
		BatteryOut batteryOut = this.getDataFromBattery();
		SensorOut sensorOut = this.getDataFromSensor(batteryOut);
		float T = sensorOut.getTemperature();
		Exception expection = new Exception();
		int result = 0;
		int brokenCell = getBrokenCell();
		if (brokenCell == 0) {

			if (T <= 70 && T >= 30) {

				this.storeDataToMemory(sensorOut);
				getDataFromCPU();
				if (PresentCapacity_me <= 18000 && PresentCapacity_me >= 0
						) {
					cpuOut = new CPUOut(PresentCapacity_me, InCA_me);
					int RUL = cpuOut.getRUL();
			
						result = RUL;

			

				} else {
					result = expection.memoryDataWrong;
				}
			} else {
				result = expection.temperatureFail;
			}
		} else {
			switch (brokenCell) {
			case 1:
				result = expection.cellOneBroken;
				
				break;

			case 2:
				result = expection.cellTwoBroken;
				break;
			case 3:
				result = expection.cellThreeBroken;
				break;
			case 4:
				result = expection.cellFourBroken;
				break;
			case 5:
				result = expection.cellFiveeBroken;
				break;
			}
		}
		if(result<0){
		setStateOfBattery(result);
		}
		return result;
	}

	@SuppressWarnings("static-access")
	int getSOH() {
		bms= new BMS();
		BatteryOut batteryOut = this.getDataFromBattery();
		SensorOut sensorOut = this.getDataFromSensor(batteryOut);
		float T = sensorOut.getTemperature();
		Exception expection = new Exception();
		int result = 0;
		int brokenCell = getBrokenCell();
		if (brokenCell == 0) {

			if (T <= 70 && T >= 30) {

				this.storeDataToMemory(sensorOut);
				getDataFromCPU();
				if (PresentCapacity_me <= 18000 && PresentCapacity_me >= 0) {
					cpuOut = new CPUOut(PresentCapacity_me, InCA_me);
					int SOH = cpuOut.getSOH();
			
						result = SOH;

			

				} else {
					result = expection.memoryDataWrong;
				}
			} else {
				result = expection.temperatureFail;
			}
		} else {
			switch (brokenCell) {
			case 1:
				result = expection.cellOneBroken;
				break;

			case 2:
				result = expection.cellTwoBroken;
				break;
			case 3:
				result = expection.cellThreeBroken;
				break;
			case 4:
				result = expection.cellFourBroken;
				break;
			case 5:
				result = expection.cellFiveeBroken;
				break;
			}
		}
		return result;

	}

}
