package objects;

public class Room {
	private String roomNum;
	private String status;
	public Room(String roomNum, String status)
	{
		this.roomNum = roomNum;
		this.status = status;
	}
	public void setRoomNum(String roomNum)
	{
		this.roomNum = roomNum;
	}
	public String getRoomNum()
	{
		return roomNum;
	}
	public void setStatus(String status)
	{
		this.status = status;
	}
	public String getStatus()
	{
		return status;
	}

}
