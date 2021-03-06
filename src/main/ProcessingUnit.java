package main;

import soc.BatteryReport;
import soc.ReportObservable;
import soh.SOHObserver;
import soh.SOHSystem;


public class ProcessingUnit extends Thread implements ProcessingUnitInterface, ReportObservable, SOHObserver {

    //Defining Inputs from Sensors
    private Float kmLeftInBattery;
    private Float currentDistanceTraveled;        //This is the distance that Car traveled in current session
    private Float nextNearestPumpDistance;
    private CarSensor cs;
    public GPSStub gps;
    
    //Alerts of Processing Unit
    private Alert alert;


    private Float speed;
    private Float currentLoopTravelTime = (float) 1;        //Time in hours
    private Float consumptionRate;
    
    //Objects of Observers
    private BatteryReport batteryReportCharge;
    
    private SOHSystem sohSystem;


    ProcessingUnit(BatteryReport batteryReportCharge, SOHSystem sohSystem) throws ValueOutOfBoundException {
    	
    	//Adding reference to own class
    	this.batteryReportCharge=batteryReportCharge;
    	this.batteryReportCharge.addObserver(this);
    	
    	this.sohSystem = sohSystem;
    	this.sohSystem.addObserver(this);
    	
    	//Other required fields
        cs = new CarSensor(5.5f);
        gps = new GPSStub(1000f);
        currentDistanceTraveled = 0f;
        alert = Alert.NO_ALERT;

        speed = (Float) BMS.getDataInCollection(BMS.CAR_SPEED);
        consumptionRate = (Float) BMS.getDataInCollection(BMS.CAR_LOAD);

        nextNearestPumpDistance = gps.getNextNearestPumpDistance();
    }

    public Float getSpeed() {
        return this.speed;
    }

    public void setSpeed(Float _speed) throws ValueOutOfBoundException {
        if (_speed < 0)  {
            throw new ValueOutOfBoundException("Car speed value in Negative");
        }
        else
        {speed = _speed;}
    }

    public void setCarLoad(Float _carLoad) throws ValueOutOfBoundException {
       if(_carLoad<0)
       { throw new ValueOutOfBoundException("Consumption value in Negative");}
       else
    	{this.consumptionRate = _carLoad;}
    }

    @Override
    public Integer getChargingCyclesLeft() {
        // TODO Auto-generated method stub
        Integer a = (Integer) BMS.getDataInCollection(BMS.CHARGING_CYCLES);
        Integer b = (Integer) BMS.getDataInCollection(BMS.CHARGING_CYCLES_USED);
        return ((Integer) BMS.getDataInCollection(BMS.CHARGING_CYCLES) - (Integer) BMS.getDataInCollection(BMS.CHARGING_CYCLES_USED));
    }


    //Calculating this distance after every 1 sec
    public void setDistanceTravelledByCar() throws ValueOutOfBoundException {
        float previousDistance = (Float) BMS.getDataInCollection(BMS.DISTANCE_TRAVELLED);



        currentDistanceTraveled = (speed * currentLoopTravelTime);
        if (currentDistanceTraveled > this.getDistanceLeftInBattery()) {
            currentDistanceTraveled = this.getDistanceLeftInBattery();
        }
        BMS.storeDataInCollection(BMS.DISTANCE_TRAVELLED, (currentDistanceTraveled + previousDistance));

        nextNearestPumpDistance = nextNearestPumpDistance - currentDistanceTraveled;
        gps.setNextNearestPumpDistance(nextNearestPumpDistance);

        //this.updateBatteryChargeLevelLeft(currentDistanceTraveled);

    }


    public Float getDistanceLeftInBattery() throws ValueOutOfBoundException {

        Float chargeInBattery = (Float) BMS.getDataInCollection(BMS.BATTERY_CHARGE_AMOUNT); // Dummy Variable, need to insert the function which will get charge from Charge group

        return chargeInBattery / (consumptionRate);
    }


    public Float getTimeLeftInBattery() throws ValueOutOfBoundException {
        Float distanceLeft = getDistanceLeftInBattery(); // Dummy Variable, need to insert the function which will get charge from Charge group
        return (distanceLeft) / (speed);
    }


    public void storeChargingBatteryLevel() {
        // TODO Auto-generated method stub
        Integer batteryLevel = (Integer) BMS.getDataInCollection(BMS.BATTERY_LEVEL) + 10;    // Dummy Variable, need to insert the function which will get battery level from charge group
        BMS.storeDataInCollection(BMS.BATTERY_LEVEL, batteryLevel);
    }


    public void showBatteryLevel() {
        // TODO Auto-generated method stub
        System.out.println("Battery Level : " + BMS.getDataInCollection(BMS.BATTERY_LEVEL));
    }


    //Dummy Stub For Charge Group
    public void updateBatteryChargeLevelLeft(Float _distanceTravelled) throws ValueOutOfBoundException {
        // TODO Auto-generated method stub
        Float chargeLeft = ((Float) BMS.getDataInCollection(BMS.BATTERY_CHARGE_AMOUNT)) - (consumptionRate * _distanceTravelled);

        BMS.storeDataInCollection(BMS.BATTERY_CHARGE_AMOUNT, chargeLeft);

    }


    @Override
    public Integer showAlerts(Alert alert) {
        // TODO Auto-generated method stub
    	int returnVal=0;
    	if(alert.getType()>0)
    	{
    		
    		System.out.println("\n--------------- ALERT ------------");
	        if ((alert.toString()).equals((Alert.ALERT_BATTERYLOW).toString())) {
	            System.out.println("\n\nBattery Low");
	            returnVal=1;
	        } else if ((alert.toString()).equals((Alert.ALERT_OVERCHARGE).toString())) {
	            System.out.println("\n\nBattery Overcharge");
	            returnVal=2;
	        } else if ((alert.toString()).equals((Alert.ALERT_HIGHTEMP).toString())) {
	            System.out.println("\n\nBattery has High Temperature");
	            returnVal=3;
	        } else if ((alert.toString()).equals((Alert.ALERT_DAMAGE).toString())) {
	            System.out.println("\n\nBattery is damages. Please replace.");
	            returnVal=4;
	        } 
	        System.out.println("\n---------- ALERT Finished------------\n");
	        
    	}
    	return returnVal;
    }
    
    
    
    
    //For implementing alert sent from Charge group
    @Override
	public void update() {
		if((this.batteryReportCharge.getAlert().toString()).equals((soc.Alert.OVER_DISCHARGE).toString()))
		{
			alert=Alert.ALERT_BATTERYLOW;
		}
		else if((this.batteryReportCharge.getAlert().toString()).equals((soc.Alert.OVERCHARGE).toString()))
		{
			alert=Alert.ALERT_OVERCHARGE;
		}
		else if((this.batteryReportCharge.getAlert().toString()).equals((soc.Alert.OVERHEATING).toString()))
		{
			alert=Alert.ALERT_HIGHTEMP;
		}
    	// TODO Auto-generated method stub
		showAlerts(alert);
		
	}
    
  //For implementing alert sent from Health group
    @Override
	public void updateSOH() {
		// TODO Auto-generated method stub
    	if(this.sohSystem.getStateOfBattery()==soh.Exception.BATTERYDAMAGE)
    	{
    		alert=Alert.ALERT_DAMAGE;
    		BMS.setBMSStatus(BMSState.DAMAGED);
    	}
    	showAlerts(alert);
		
	}
    
    


    public void execute() {
        try {
            if (!alert.toString().equals(Alert.ALERT_DAMAGE.toString())) {

                if (BMS.getBMSStatus().equals(BMSState.ONMOVE.toString())) {


                    System.out.println("\n\n------ Presenting GUI Output -------\n");

                    //storeBatteryLevel();
                    setDistanceTravelledByCar();

                    System.out.format("Car Speed : %.2f Km/hr \n",speed);
                    showBatteryLevel();
                    System.out.println("Charge Amount : " + BMS.getDataInCollection(BMS.BATTERY_CHARGE_AMOUNT));
                    System.out.format("Distance Left : %.2f Km \n",getDistanceLeftInBattery());
                    System.out.format("Total Distance travelled : %.2f Km \n", BMS.getDataInCollection(BMS.DISTANCE_TRAVELLED));
                    System.out.format("Time Left for next charge : %.2f hours \n", getTimeLeftInBattery());
                    System.out.println("Charging Cycles left : " + getChargingCyclesLeft());

                    System.out.format("Distance to next Pump : %.2f Km \n", Math.abs(gps.getNextNearestPumpDistance()));
                    
                    System.out.format("Battery Temperature : %.1f degree celcius \n",BMS.getDataInCollection(BMS.CURRENT_BATTERY_TEMPERATURE));
                    System.out.format("Battery Health : %d %% \n",(Integer)BMS.getDataInCollection(BMS.BATTERY_HEALTH));
                    System.out.format("Remaining Useful Life : %d days \n",(Integer)BMS.getDataInCollection(BMS.BATTERY_LIFE));

                    /*if (alert.getType() > 0) {
                        showAlerts(alert);
                    }*/


                    System.out.println("\n------ GUI Output End -------\n\n");
                    
                    printSystemLog();
                } else if (BMS.getBMSStatus().equals(BMSState.CHARGING.toString())) {
                    System.out.println("------ Presenting GUI Output -------\n");

                    storeChargingBatteryLevel();

                    showBatteryLevel();
                    System.out.println("Charge Amount : " + BMS.getDataInCollection(BMS.BATTERY_CHARGE_AMOUNT));
                    System.out.println("Charging Cycles left : " + getChargingCyclesLeft());

                    /*if (alert.getType() > 0) {
                        showAlerts(alert);
                    }*/

                    System.out.println("\n------ GUI Output End -------\n\n");
                }
            } 
        } catch (ValueOutOfBoundException exception) {
            System.err.println("Exception occured 1 : " + exception.getMessage());
        } catch (Exception e) {
            System.err.println("Exception occured 2 : " + e.getStackTrace() +
                    e.getMessage());
        }
    }
    
    
    
    //Function to print Log information of the system
    public void printSystemLog()
    {
    	System.out.println("--------------- System Log Information --------------");
    	
    	//Log from Charge Group
    	
    		
    	
    	//Log from Control Group
    	System.out.println("Charge for each Cell in the battery");
			System.out.println("\tCell 1: " + BMS.getDataInCollection(BMS.CHARGE_AMOUNT_CELL1));
			System.out.println("\tCell 2: " + BMS.getDataInCollection(BMS.CHARGE_AMOUNT_CELL2));
			System.out.println("\tCell 3: " + BMS.getDataInCollection(BMS.CHARGE_AMOUNT_CELL3));
			System.out.println("\tCell 4: " + BMS.getDataInCollection(BMS.CHARGE_AMOUNT_CELL4));
			System.out.println("\tCell 5: " + BMS.getDataInCollection(BMS.CHARGE_AMOUNT_CELL5));
		
		System.out.println("\n\nLoad/Consumption Rate:" + BMS.getDataInCollection(BMS.CAR_LOAD));
    	//Log from Health Group
		System.out.println("Battery Capacity: " + BMS.getDataInCollection(BMS.PRESENTCAPACITY)+" MHA");
		System.out.println("Remaining Useful Life : " + BMS.getDataInCollection(BMS.BATTERY_LIFE)+" days");
		System.out.println("Battery Healthy : " + BMS.getDataInCollection(BMS.BATTERY_HEALTH)+" %");
    	System.out.println("--------------- System Log Information Ends --------------");
    	
    }
    
    
    
    
    @Override
    public void run()
    {
    	do {

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            this.execute();

        }
        while ((Float) BMS.getDataInCollection(BMS.BATTERY_CHARGE_AMOUNT) > 0 && (Integer) BMS.getDataInCollection(BMS.BATTERY_LEVEL) < 100 && !BMS.getBMSStatus().toString().equals(BMSState.DAMAGED.toString()));
    	
    	BMS.BMS_STATE=BMSState.IDLE;
    }

	


}
