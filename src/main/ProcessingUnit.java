package pg;
public class ProcessingUnit implements ProcessingUnitInterface {
	
	//Defining Inputs from Sensors
	private Float kmLeftInBattery;
	private Float currentDistanceTraveled;		//This is the distance that Car traveled in current session
	private Float nextNearestPumpDistance;
	private CarSensor cs;
	private GPSStub gps;
	private Integer alert;
	
	
	private Float speed;
	private Float currentLoopTravelTime=(float)1;		//Time in hours
	private Float consumptionRate;
	
	
	ProcessingUnit() throws ValueOutOfBoundException
	{
		cs=new CarSensor(5.5f,50f);
		gps=new GPSStub(200f);
		currentDistanceTraveled=0f;
		alert=Alert.NO_ALERT;
		
		speed=(Float)BMS.getDataInCollection(BMS.CAR_SPEED);
		consumptionRate=(Float)BMS.getDataInCollection(BMS.CAR_LOAD);
		
		nextNearestPumpDistance=200f;
	}
	
	
	public void setSpeed(Float _speed)
	{
		speed=_speed;
	}
	
	public Float getSpeed()
	{
		return this.speed;
	}
	
	public void setCarLoad(Float _carLoad)
	{
		this.consumptionRate=_carLoad;
	}
	
	@Override
	public Integer getChargingCyclesLeft() {
		// TODO Auto-generated method stub
		Integer a=(Integer)BMS.getDataInCollection(BMS.CHARGING_CYCLES);
		Integer b=(Integer)BMS.getDataInCollection(BMS.CHARGING_CYCLES_USED);
		return ((Integer)BMS.getDataInCollection(BMS.CHARGING_CYCLES)-(Integer)BMS.getDataInCollection(BMS.CHARGING_CYCLES_USED));
	}
	
	
	//Calculating this distance after every 1 sec
	public void setDistanceTravelledByCar() throws ValueOutOfBoundException
	{
			float previousDistance = (Float)BMS.getDataInCollection(BMS.DISTANCE_TRAVELLED);
			
			if(speed<0 || speed==null)
			{
				throw new ValueOutOfBoundException("Car speed value in Negative");
			}
			
			if(nextNearestPumpDistance<0)
			{
				throw new ValueOutOfBoundException("Distance for next station has negative value");
			}
			
			
				currentDistanceTraveled=(speed*currentLoopTravelTime);
				if(currentDistanceTraveled > this.getDistanceLeftInBattery())
				{
					currentDistanceTraveled=this.getDistanceLeftInBattery();
				}
				BMS.storeDataInCollection(BMS.DISTANCE_TRAVELLED,(currentDistanceTraveled+previousDistance));
				
				nextNearestPumpDistance = nextNearestPumpDistance - currentDistanceTraveled;
				
				System.out.println("Distance to Next Pummp Temp : "  + (nextNearestPumpDistance));
				
			this.updateBatteryChargeLevelLeft(currentDistanceTraveled);
			
	}

	
	
	
	
	
	public Float getDistanceLeftInBattery() throws ValueOutOfBoundException
	{
		if(consumptionRate<0)
		{
			throw new ValueOutOfBoundException("Consumption rate value in Negative");
		}
		Float chargeInBattery=(Float)BMS.getDataInCollection(BMS.BATTERY_CHARGE_AMOUNT); // Dummy Variable, need to insert the function which will get charge from Charge group
		
		return chargeInBattery/(consumptionRate);
	}
	

	
	public Float getTimeLeftInBattery() throws ValueOutOfBoundException
	{
		if(speed<0)
		{
			throw new ValueOutOfBoundException("Car speed value in Negative");
		}
		Float distanceLeft=(Float)getDistanceLeftInBattery(); // Dummy Variable, need to insert the function which will get charge from Charge group
		return (distanceLeft)/(speed);
	}

	
	@Override
	public void storeBatteryLevel() {
		// TODO Auto-generated method stub
		Float batteryLevel=(Float) BMS.getDataInCollection(BMS.BATTERY_LEVEL)-10;	// Dummy Variable, need to insert the function which will get battery level from charge group
		BMS.storeDataInCollection(BMS.BATTERY_LEVEL, batteryLevel);
	}
	
	
	public void storeChargingBatteryLevel() {
		// TODO Auto-generated method stub
		Float batteryLevel=(Float) BMS.getDataInCollection(BMS.BATTERY_LEVEL)+10;	// Dummy Variable, need to insert the function which will get battery level from charge group
		BMS.storeDataInCollection(BMS.BATTERY_LEVEL, batteryLevel);
	}
	
	
	
	public void showBatteryLevel() {
		// TODO Auto-generated method stub
		System.out.println("Battery Level : " + (Float) BMS.getDataInCollection(BMS.BATTERY_LEVEL));
	}

	
	
	@Override
	public void storeCarSpeed(Float speed) {
		// TODO Auto-generated method stub
		try {
			cs.updateCarSpeed(speed);
		} catch (ValueOutOfBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	//Dummy Stub For Charge Group
	public void updateBatteryChargeLevelLeft(Float _distanceTravelled) throws ValueOutOfBoundException {
		// TODO Auto-generated method stub
		if(consumptionRate<0 || consumptionRate==null)
		{
			throw new ValueOutOfBoundException("Consumption rate value in Negative");
		}
		
		Float chargeLeft=((Float)BMS.getDataInCollection(BMS.BATTERY_CHARGE_AMOUNT)) - (consumptionRate*_distanceTravelled);
		
		BMS.storeDataInCollection(BMS.BATTERY_CHARGE_AMOUNT, chargeLeft);
		
	}
	
	
	
	
	@Override
	public Integer showAlerts(Integer alert) {
		// TODO Auto-generated method stub
		if(alert == Alert.ALERT_BATTERYLOW)
		{
			System.out.println("--------------- ALERT ------------\n\nBattery Low");
			return 1;
		}
		else if(alert == Alert.ALERT_OVERCHARGE)
		{
			System.out.println("--------------- ALERT ------------\n\nBattery Overcharge");
			return 2;
		}
		else if(alert == Alert.ALERT_HIGHTEMP)
		{
			System.out.println("--------------- ALERT ------------\n\nBattery has High Temperature");
			return 3;
		}
		else if(alert == Alert.ALERT_DAMAGE)
		{
			System.out.println("--------------- ALERT ------------\n\nBattery is damages. Please replace.");
			return 4;
		}
		else
		{
			return 0;
		}
	}
	
	
	

	public void execute()
	{
		try
		{
			if(alert!=Alert.ALERT_DAMAGE)
			{
				
				if(BMS.getBMSStatus().equals(BMSState.ONMOVE.toString()))
				{
					
				
					System.out.println("------ Presenting GUI Output -------\n");
					
					storeBatteryLevel();
					setDistanceTravelledByCar();
					
					System.out.println("Car Speed : " + speed);
					showBatteryLevel();
					System.out.println("Charge Amount : " +BMS.getDataInCollection(BMS.BATTERY_CHARGE_AMOUNT));
					System.out.println("Distance Left : " + getDistanceLeftInBattery());
					System.out.println("Total Distance travelled : " + BMS.getDataInCollection(BMS.DISTANCE_TRAVELLED));
					System.out.println("Time Left for next charge : " + getTimeLeftInBattery());
					System.out.println("Charging Cycles left : " + getChargingCyclesLeft());
					
					System.out.println("Distance to next Pump : " + nextNearestPumpDistance);
					
					if(alert>0)
					{
						showAlerts(alert);
					}
					
					
					
					
					System.out.println("\n------ GUI Output End -------\n\n");
				}
				else if(BMS.getBMSStatus().equals(BMSState.CHARGING.toString()))
				{
					System.out.println("------ Presenting GUI Output -------\n");
					
					storeChargingBatteryLevel();
					
					showBatteryLevel();
					System.out.println("Charge Amount : " +BMS.getDataInCollection(BMS.BATTERY_CHARGE_AMOUNT));
					System.out.println("Charging Cycles left : " + getChargingCyclesLeft());
					
					if(alert>0)
					{
						showAlerts(alert);
					}
					
					System.out.println("\n------ GUI Output End -------\n\n");
				}
			}
			else
			{
				System.out.println("--------------- ALERT ------------\n\nBattery is damages. Please replace.");
			}
		}
		catch(ValueOutOfBoundException exception)
		{
			System.err.println("Exception occured 1 : " + exception.getMessage());
		}
		catch(Exception e)
		{
			System.err.println("Exception occured 2 : " + e.getStackTrace() + 
					e.getMessage());
		}
	}

	
	

}
