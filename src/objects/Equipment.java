package objects;

public class Equipment {
	private String associatedRoomNum;
	private String name;
	private boolean isWorking;
	public Equipment(String associatedRoomNum, String name)
	{
		this.associatedRoomNum = associatedRoomNum;
		this.name = name;
		this.isWorking = true;
	}
	public void setAssociatedRoomNum(String associatedRoomNum)
	{
		this.associatedRoomNum = associatedRoomNum;
	}
	public String getAssociatedRoomNum()
	{
		return associatedRoomNum;
	}
	public String getName()
	{
		return name;
	}
	public void setIsWorking(boolean isWorking)
	{
		this.isWorking = isWorking;
	}
	public boolean getIsWorking()
	{
		return isWorking;
	}
}
