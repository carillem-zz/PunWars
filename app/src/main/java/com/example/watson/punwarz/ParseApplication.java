package com.example.watson.punwarz;

import android.app.Application;

import com.parse.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class ParseApplication extends Application {

    private String userObjectID = null; //the active user
    private String tempLobbyObjectID = null; //since Parse queries can't actually return???
    private String tempPostObjectID = null;
    private boolean exists = false; //all-purpose boolean for query results.
    private ParseObject activeObject = null;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void runTests(){ //Runs a series of tests with pre-determined values to test Parse functionality

    }

    //creates a new user object in Parse if the user has not previously used app.
    public void createNewUser(String facebookID, String displayName) {
        ParseObject newUser = new ParseObject("Users");
        newUser.put("UserID", facebookID);
        newUser.put("DisplayName", displayName);
        newUser.put("Score", 0);
        userObjectID = newUser.getObjectId(); //This may have to be after the save??
        newUser.saveInBackground();
    }

    //creates a new lobby associated with the active user
    public void createNewLobby(String lobbyPrompt, Date expiryDate){
        ParseObject newLobby = new ParseObject("Lobby");
        newLobby.put("Prompt", lobbyPrompt);
        newLobby.put("CreatorID", userObjectID);
        newLobby.put("ExpiryDate", expiryDate);
        newLobby.put("IsActive", true);
        newLobby.saveInBackground();
    }

    //creates a new Post associated to a particular Lobby
    public void createNewPost(String creatorID, String lobbyID, String postText){
        ParseObject newPost = new ParseObject("Posts");
        newPost.put("UserID", userObjectID);
        newPost.put("LobbyID", lobbyID);
        newPost.put("Post", postText);
        newPost.put("ExpiryDate", getParseObject("Lobby", lobbyID).getDate("ExpiryDate"));
        newPost.put("IsActive", true); //may not need this?
        newPost.saveInBackground();
        String postObjectID = newPost.getObjectId(); //may have to move this before the save??
    }

    //sets the userObjectID variable so that it does not have to be queried multiple times.
    private void setUserObjectID(final String FacebookID){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Users");
        query.whereEqualTo("UserID", FacebookID);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (object == null) {
                    //The getFirst request failed.
                } else {
                    //Retrieved the object.
                    userObjectID = object.getObjectId();
                }
            }
        });

    }

    //Checks if an object from the selected tableName, in the selected tableRow, and the searchedValue already exists
    //This is the general use function.
    private void checkIfObjectExists(String tableName, String tableRow, String searchedValue){
        ParseQuery<ParseObject> query = ParseQuery.getQuery(tableName);
        query.whereEqualTo(tableRow, searchedValue);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (object == null) {
                    //Object does not exist.
                    exists = false;
                } else {
                    //Object does exist already.
                    exists = true;
                }
            }
        });
    }

    //runs a query and returns a ParseObject for manipulation.
    private ParseObject getParseObject(String tableName, String objectID){

        ParseQuery<ParseObject> query = ParseQuery.getQuery(tableName);
        query.whereEqualTo("ObjectID", objectID);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (object == null) {
                    //Object does not exist.

                } else {
                    //Object does exist.
                    activeObject = object;
                }
            }
        });
        return activeObject;
    }


    //Checks if a given Facebook user ID is already in the Parse database
    public boolean doesUserExist(String facebookID) {
        clearTempVars();
        checkIfObjectExists("Users", "UserID", facebookID);
        if(exists == true){setUserObjectID(facebookID);} //if the user exists, they are automatically set to active.
        return exists;
    }

    //checks if a lobby with an existing prompt description already exists. currently not case sensitive
    public boolean doesLobbyExist(String description){
        clearTempVars();
        checkIfObjectExists("Lobby", "Prompt", description);
        return exists;
    }

    //checks if a post within a lobby already exists. currently not case sensitive.
    public boolean doesPostExist(String post, String lobbyID) {
        clearTempVars();
        // Posts are specific to a lobby, so a check would need the associated lobby ID
        ParseQuery<ParseObject> checkPost = ParseQuery.getQuery("Posts");
        checkPost.whereEqualTo("Post", post);

        ParseQuery<ParseObject> checkLobby = ParseQuery.getQuery("Posts");
        checkLobby.whereEqualTo("LobbyID", lobbyID);

        List<ParseQuery<ParseObject>> queries = new ArrayList<ParseQuery<ParseObject>>();
        queries.add(checkPost);
        queries.add(checkLobby);

        //TODO: figure out AND functionality for Parse Queries
        ParseQuery<ParseObject> mainQuery = ParseQuery.or(queries);
        mainQuery.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> results, ParseException e) {
                if (results.isEmpty()){exists = false;}
                else {exists = true;}
            }
        });
        return exists;
    }

    public String getLobbyObjectID(){
        return tempLobbyObjectID;
    }

    //resets the Global variables to ensure data from previous calls does not persist
    private void clearTempVars(){
        tempLobbyObjectID = null;
        tempPostObjectID = null;
        exists = false;
    }

    //Call this when logging out to clear the userObjectID
    private void deactivateUser(){
        userObjectID = null;
        activeObject = null;
        clearTempVars();
    }
}
