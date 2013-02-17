package adonai.diary_browser.entities;

import java.util.ArrayList;

public class DiscList extends Openable
{
	private ArrayList<Discussion> discussions;
	 
	public static class Discussion extends Post
	{

	}
	
	public DiscList()
	{
		setDiscussions(new ArrayList<Discussion>());
	}

	public ArrayList<Discussion> getDiscussions() 
	{
		return discussions;
	}

	public void setDiscussions(ArrayList<Discussion> discussions) 
	{
		this.discussions = discussions;
	}
}