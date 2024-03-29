package eu.kartoffelquadrat.xoxresttest;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import eu.kartoffelquadrat.xoxinternals.controller.Ranking;
import eu.kartoffelquadrat.xoxinternals.controller.XoxClaimFieldAction;
import eu.kartoffelquadrat.xoxinternals.model.Board;
import eu.kartoffelquadrat.xoxinternals.model.ModelAccessException;
import eu.kartoffelquadrat.xoxinternals.model.Player;
import eu.kartoffelquadrat.xoxinternals.model.XoxInitSettings;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.LinkedList;

/**
 * All Unit tests for Xox Resources
 */
public class XoxTest
    extends RestTestUtils {

  long sampleGameId = 42;

  /**
   * Try to retrieve list of all games. Verify the default sample game is present.
   */
  @Test
  public void testXoxGet() throws UnirestException {

    assert getAllRegisteredGameIds().contains(sampleGameId);
  }

  /**
   * Try to add new game
   */
  @Test
  public void testXoxPost() throws UnirestException {

    // Add new game
    long id = addSampleGame();

    // Verify the game exists, if and only if read verifications are requested
    if (RestTestUtils.isReadVerficationsRequested()) {
      assert getAllRegisteredGameIds().contains(id);
    } else {
      System.out.println("READ VERIFICATIONS SKIPPED TO REDUCE TEST CROSS DEPENDENCIES.");
    }
  }

  /**
   * Test access to ranking information of the default (static) sample game.
   */
  @Test
  public void testXoxIdGet() throws UnirestException, ModelAccessException {

    // Get Ranking info and analyze
    HttpResponse<String> rankingReply =
        Unirest.get(getServiceURL(Long.toString(sampleGameId))).asString();
    verifyOk(rankingReply);

    // Deserialize response payload
    Ranking ranking = new Gson().fromJson(rankingReply.getBody(), Ranking.class);

    // TODO: double check these asserts are correct for default sample game.
    // Verify ranking properties (response content)
    Assert.assertFalse(
        "Access to test game marked game over while the sample game should be still running.",
        ranking.isGameOver());
    Assert.assertTrue(
        "Max should have a score of 0 in the sample game, but the value listed by ranking object is not 0.",
        ranking.getScoreForPlayer("Max") == 0);
    Assert.assertTrue(
        "Moritz should have a score of 0 in the sample game, but the value listed by ranking object is not 0.",
        ranking.getScoreForPlayer("Moritz") == 0);
  }

  /**
   * Try to delete the sample game
   *
   * @throws UnirestException
   */
  @Test
  public void testXoxIdDelete() throws UnirestException {

    // Delete game
    HttpResponse<String> deleteGameReply =
        Unirest.delete(getServiceURL(Long.toString(sampleGameId))).asString();
    verifyOk(deleteGameReply);

    // Verify the game exists, if and only if read verifications are requested
    if (RestTestUtils.isReadVerficationsRequested()) {
      // Verify game is no longer there
      Assert.assertFalse("Deleted test game, but the list of existing game still contains its ID.",
          getAllRegisteredGameIds().contains(sampleGameId));
    } else {
      System.out.println("READ VERIFICATIONS SKIPPED TO REDUCE TEST CROSS DEPENDENCIES.");
    }
  }

  /**
   * Test to verify if endpoint for board retrieval works as expected.
   *
   * @throws UnirestException
   */
  @Test
  public void testXoxIdBoardGet() throws UnirestException {

    // Verify board layout (empty)
    HttpResponse<String> getBoardResponse =
        Unirest.get(getServiceURL(Long.toString(sampleGameId) + "/board")).asString();
    verifyOk(getBoardResponse);
    Board board = new Gson().fromJson(getBoardResponse.getBody(), Board.class);

    // TODO: double check these asserts make sense for default game state of static sample game.
    // Verify board status (must be empty board)
    Assert.assertTrue("Sample board should be empty, but corresponding flag is false.",
        board.isEmpty());
    for (int x = 0; x < 3; x++) {
      for (int y = 0; y < 3; y++) {
        Assert.assertTrue(board.isFree(x, y));
      }
    }
    Assert.assertFalse(
        "Board should not have three in a line, but the corresponding field is set to true",
        board.isThreeInALine());
  }

  /**
   * Try to retrieve player info for sample xox game instance.
   *
   * @throws UnirestException in case of communication error with xox backend.
   */
  @Test
  public void testXoxIdPlayersGet() throws UnirestException {

    // Access players resource
    HttpResponse<String> getPlayersResponse =
        Unirest.get(getServiceURL(sampleGameId + "/players")).asString();
    verifyOk(getPlayersResponse);
    Player[] players = new Gson().fromJson(getPlayersResponse.getBody(), Player[].class);


    // TODO: enasure these asserts make sense for default sample game.
    Assert.assertTrue("Not exactly two players in sample game.", players.length == 2);
    Assert.assertTrue("First player not Max", players[0].getName().equals("Max"));
    Assert.assertTrue("First player colour not #CAFFEE",
        players[0].getPreferredColour().equals("#CAFFEE"));
    Assert.assertTrue("Second player not Moritz", players[1].getName().equals("Moritz"));
    Assert.assertTrue("Second player colour not #1CE7EA",
        players[1].getPreferredColour().equals("#1CE7EA"));
  }

  /**
   * For both players, retrieve the array of action objects (sample game) and check for length.
   *
   * @throws UnirestException
   */
  @Test
  public void testXoxIdPlayersIdActionsGet() throws UnirestException {

    // Access players resource, parse response to hash indexed map
    HttpResponse<String> getActionsResponsePlayer1 =
        Unirest.get(getServiceURL(sampleGameId + "/players/Max/actions")).asString();
    verifyOk(getActionsResponsePlayer1);
    XoxClaimFieldAction[] actionsPlayer1 = new Gson().fromJson(getActionsResponsePlayer1.getBody(),
        new XoxClaimFieldAction[] {}.getClass());

    // TODO: Ensure these asserts make sense for sample game.
    // All 9 fields must be accessible, there should be 9 entries in hashmap
    Assert.assertTrue(
        "Retrieved actions bundle does not contain 9 entries, while the xox board is empty",
        actionsPlayer1.length == 9);

    // Do the same for player 2 (not their turn) resource, parse response to hash indexed map
    HttpResponse<String> getActionsResponsePlayer2 =
        Unirest.get(getServiceURL(sampleGameId + "/players/Moritz/actions"))
            .asString();
    verifyOk(getActionsResponsePlayer2);
    XoxClaimFieldAction[] actionsPlayer2 = new Gson().fromJson(getActionsResponsePlayer2.getBody(),
        new XoxClaimFieldAction[] {}.getClass());

    // action map for player 2 must be empty (not their turn)
    Assert.assertTrue(
        "Retrieved actions bundle does not contain 0 entries, while it is not player 2s turn.",
        actionsPlayer2.length == 0);
  }

  /**
   * Test placing a marker on the board, by sending a post for the corresponding hash value
   *
   * @throws UnirestException
   */
  @Test
  public void testXoxIdPlayersIdActionsActionPost() throws UnirestException {

    // Access players resource, parse response to hash indexed map
    HttpResponse<String> postActionResponse =
        Unirest.post(getServiceURL(sampleGameId + "/players/Max/actions/0")).asString();
    verifyOk(postActionResponse);

    // If validation of effectiveness requested, also verify resulting game state
    if (RestTestUtils.isReadVerficationsRequested()) {
      // Access players resource, parse response to hash indexed map
      HttpResponse<String> getActionsResponsePlayer2 =
          Unirest.get(getServiceURL(sampleGameId + "/players/Moritz/actions")).asString();
      verifyOk(getActionsResponsePlayer2);
      XoxClaimFieldAction[] actionsPlayer2 =
          new Gson().fromJson(getActionsResponsePlayer2.getBody(),
              new XoxClaimFieldAction[] {}.getClass());

      // Remaining 8 fields must be accessible, there should be 9 entries in hashmap
      Assert.assertTrue(
          "Retrieved actions bundle does not contain 9 entries, while the xox board is empty",
          actionsPlayer2.length == 8);

      // Do the same for player 1 (not their turn) resource, parse response to hash indexed map
      HttpResponse<String> getActionsResponsePlayer1 =
          Unirest.get(getServiceURL(sampleGameId + "/players/Max/actions")).asString();
      verifyOk(getActionsResponsePlayer1);
      XoxClaimFieldAction[] actionsPlayer1 =
          new Gson().fromJson(getActionsResponsePlayer1.getBody(),
              new XoxClaimFieldAction[] {}.getClass());

      // action map for player 2 must be empty (not their turn)
      Assert.assertTrue(
          "Retrieved actions bundle does not contain 0 entries, while it is not player 1s turn.",
          actionsPlayer1.length == 0);
    } else {
      System.out.println("READ VERIFICATIONS SKIPPED TO REDUCE TEST CROSS DEPENDENCIES.");
    }
  }

  /**
   * Helper method to look up list of all registered game IDs as collection.
   *
   * @return
   */
  private LinkedList<Long> getAllRegisteredGameIds() throws UnirestException {

    HttpResponse<String> allGamesResponse = Unirest.get(getServiceURL("")).asString();
    verifyOk(allGamesResponse);

    // Verify default sample game is present
    String allGamesString = allGamesResponse.getBody();
    Type listType = new TypeToken<LinkedList<Long>>() {
    }.getType();
    LinkedList<Long> allGameIds = new Gson().fromJson(allGamesString, listType);
    return allGameIds;
  }

  /**
   * Helper method to add a new sample game to the backend.
   *
   * @return HttpResponse<String> that encodes server reply.
   */
  private long addSampleGame() throws UnirestException {
    // Try to add new Game
    LinkedList<Player> players = new LinkedList<>();
    players.add(new Player("Max", "#CAFFEE"));
    players.add(new Player("Moritz", "#1CE7EA"));
    XoxInitSettings testSettings = new XoxInitSettings(players, "Max");

    // String JSON-encoded testSettings:
    String jsonTestSettings = new Gson().toJson(testSettings);
    HttpResponse<String> addGameResponse =
        Unirest.post(getServiceURL("")).header("Content-Type", "application/json")
            .body(jsonTestSettings).asString();
    verifyOk(addGameResponse);
    return Long.parseLong(addGameResponse.getBody());
  }
}
