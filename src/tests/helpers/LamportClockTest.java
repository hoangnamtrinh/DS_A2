package tests.helpers;

import main.helpers.LamportClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LamportClockTest {
  private LamportClock lamportClock;

  @BeforeEach
  public void setUp() {
    lamportClock = new LamportClock();
  }

  @Test
  public void testInitialTimeIsZero() {
    assertEquals(0, lamportClock.getCurrentTime());
  }

  @Test
  public void testIncrement() {
    lamportClock.increment();
    assertEquals(1, lamportClock.getCurrentTime());
  }

  @Test
  public void testSendLCTime() {
    int currentTime = lamportClock.sendLCTime();
    assertEquals(1, currentTime);
    assertEquals(1, lamportClock.getCurrentTime());
  }

  @Test
  public void testReceiveLCTime() {
    int receivedTime = 3;
    lamportClock.receiveLCTime(receivedTime);
    assertEquals(receivedTime + 1, lamportClock.getCurrentTime());
  }

  @Test
  public void testReceiveLCTimeWithSmallerTime() {
    int receivedTime = 1;
    lamportClock.receiveLCTime(receivedTime);
    assertEquals(2, lamportClock.getCurrentTime());
  }

  @Test
  public void testSetCurrentTime() {
    int newTime = 5;
    lamportClock.setCurrentTime(newTime);
    assertEquals(newTime, lamportClock.getCurrentTime());
  }
}
