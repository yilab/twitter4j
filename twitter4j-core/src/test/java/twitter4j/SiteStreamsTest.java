/*
Copyright (c) 2007-2011, Yusuke Yamamoto
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the Yusuke Yamamoto nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY Yusuke Yamamoto ``AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL Yusuke Yamamoto BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package twitter4j;

import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationContext;
import twitter4j.conf.PropertyConfiguration;
import twitter4j.internal.async.DispatcherFactory;
import twitter4j.json.DataObjectFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * @author Yusuke Yamamoto - yusuke at mac.com
 * @since Twitter4J 2.1.8
 */
public class SiteStreamsTest extends TwitterTestBase implements SiteStreamsListener {
    public SiteStreamsTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testStream() throws Exception {
        InputStream is = SiteStreamsTest.class.getResourceAsStream("/sitestream-testcase.json");
        SiteStreamsImpl siteStreams = new SiteStreamsImpl(new DispatcherFactory(ConfigurationContext.getInstance()).getInstance(), is, conf1);
        SiteStreamsListener[] listeners = new SiteStreamsListener[1];
        listeners[0] = this;
        received.clear();
        siteStreams.next(listeners);
        synchronized (this) {
            this.wait(200);
        }
        assertEquals("onfriendlist", received.get(0)[0]);
        assertEquals(6358482, received.get(0)[1]);
        received.clear();
        siteStreams.next(listeners);
        synchronized (this) {
            this.wait(200);
        }
        assertEquals("onfriendlist", received.get(0)[0]);
        assertEquals(new Integer(6358481), received.get(0)[1]);
        received.clear();
        siteStreams.next(listeners);
        synchronized (this) {
            this.wait(200);
        }
        assertEquals("onfriendlist", received.get(0)[0]);
        assertEquals(new Integer(4933401), received.get(0)[1]);
    }

    public void testSiteStream() throws Exception {
        InputStream is = SiteStreamsTest.class.getResourceAsStream("/sitestream-test.properties");
        if (null == is) {
            System.out.println("sitestream-test.properties not found. skipping Site Streams test.");
        } else {
            Properties props = new Properties();
            props.load(is);
            is.close();
            Configuration yusukeyConf = new PropertyConfiguration(props, "/yusukey");
            Configuration twit4jConf = new PropertyConfiguration(props, "/twit4j");
            Configuration twit4j2Conf = new PropertyConfiguration(props, "/twit4j2");
            TwitterStream twitterStream = new TwitterStreamFactory(yusukeyConf).getInstance();
            twitterStream.addListener(this);
            Twitter twit4j = new TwitterFactory(twit4jConf).getInstance();
            Twitter twit4j2 = new TwitterFactory(twit4j2Conf).getInstance();
            try {
                twit4j.destroyBlock(6377362);
            } catch (TwitterException ignore) {
            }
            try {
                twit4j2.destroyBlock(6358482);
            } catch (TwitterException ignore) {
            }
            try {
                twit4j.createFriendship(6377362);
            } catch (TwitterException ignore) {
            }
            try {
                twit4j2.createFriendship(6358482);
            } catch (TwitterException ignore) {
            }

            //twit4j: 6358482
            //twit4j2: 6377362
            twitterStream.site(true, new int[]{6377362, 6358482});
            //expecting onFriendList for twit4j and twit4j2
            waitForStatus();
            waitForStatus();

            Status status = twit4j2.updateStatus("@twit4j " + new Date());
            //expecting onStatus for twit4j from twit4j
            waitForStatus();

            twit4j.createFavorite(status.getId());
            waitForStatus();

            twit4j.destroyFavorite(status.getId());
            waitForStatus();

            // unfollow twit4j
            twit4j2.destroyFriendship(6358482);
            waitForStatus();

            // follow twit4j
            twit4j2.createFriendship(6358482);
            waitForStatus();

            // unfollow twit4j2
            twit4j.destroyFriendship(6377362);

            // follow twit4j2
            twit4j.createFriendship(6377362);
            waitForStatus();

            twit4j.retweetStatus(status.getId());
            waitForStatus();
            DirectMessage dm = twit4j.sendDirectMessage(42419133, "test " + new Date());
            waitForStatus();

            twitter2.destroyStatus(status.getId());
            waitForStatus();

            twitter1.destroyDirectMessage(dm.getId());
            waitForStatus();

            // block twit4j
            twit4j2.createBlock(6358482);
            waitForStatus();

            // unblock twit4j
            twit4j2.destroyBlock(6358482);
            waitForStatus();

            try {
                twit4j.createFriendship(6377362);
                waitForStatus();
            } catch (TwitterException ignore) {
            }
            try {
                twit4j2.createFriendship(6358482);
                waitForStatus();
            } catch (TwitterException ignore) {
            }
            twitter1.updateProfile(null, null, new Date().toString(), null);
            waitForStatus();

            UserList list = twit4j.createUserList("test", true, "desctription");
            waitForStatus();
            list = twit4j.updateUserList(list.getId(), "test2", true, "description2");
            waitForStatus();
            twit4j.addUserListMember(list.getId(), id2.id);
            waitForStatus();
            twit4j2.subscribeUserList("twit4j", list.getId());
            waitForStatus();
            twit4j2.unsubscribeUserList("twit4j", list.getId());
            waitForStatus();
            twit4j.deleteUserListMember(list.getId(), id2.id);
            waitForStatus();
            twit4j.destroyUserList(list.getId());
            waitForStatus();

            assertReceived("onstatus", "onstatus");
            assertReceived("onfriendlist", "onfriendlist");
            assertReceived("onFavorite", TwitterMethod.CREATE_FAVORITE);
            assertReceived("onUnfavorite", TwitterMethod.DESTROY_FAVORITE);
//            assertReceived(TwitterMethod.RETWEET_STATUS);
            assertReceived("onDirectMessage", TwitterMethod.SEND_DIRECT_MESSAGE);

            assertReceived("onDeletionNotice-status", TwitterMethod.DESTROY_STATUS);
            assertReceived("onDeletionNotice-directmessage", TwitterMethod.DESTROY_DIRECT_MESSAGE);

            assertReceived("onUserListMemberAddition", TwitterMethod.ADD_LIST_MEMBER);
            assertReceived("onUserListMemberDeletion", TwitterMethod.DELETE_LIST_MEMBER);
            assertReceived("onUserListSubscribed", TwitterMethod.SUBSCRIBE_LIST);
            assertReceived("onUserListUnsubscribed", TwitterMethod.UNSUBSCRIBE_LIST);
            assertReceived("onUserListCreated", TwitterMethod.CREATE_USER_LIST);
            assertReceived("onUserListUpdated", TwitterMethod.UPDATE_USER_LIST);
            assertReceived("onUserListDestoyed", TwitterMethod.DESTROY_USER_LIST);


            assertReceived("onUserProfileUpdated", TwitterMethod.UPDATE_PROFILE);

            assertReceived("onBlock", TwitterMethod.CREATE_BLOCK);
            assertReceived("onUnblock", TwitterMethod.DESTROY_BLOCK);
            assertReceived("onFollow", TwitterMethod.CREATE_FRIENDSHIP);
            assertReceived("onUnfollow", TwitterMethod.DESTROY_FRIENDSHIP);
        }
    }

    private void assertReceived(String assertion, Object obj) {
        boolean received = false;
        for (Object[] event : this.received) {
            if (obj.equals(event[0])) {
                received = true;
                break;
            }
        }
        assertTrue(assertion, received);
    }

//    public void testSiteStreamPull() throws Exception {
//        InputStream is = SiteStreamTest.class.getResourceAsStream("/sitestream-test.properties");
//        if (null == is) {
//            System.out.println("sitestream-test.properties not found. skipping Site Streams test.");
//        } else {
//            Properties props = new Properties();
//            props.load(is);
//            is.close();
//            Configuration conf = new PropertyConfiguration(props,"/yusukey");
//            TwitterStream twitterStream = new TwitterStreamFactory(conf).getInstance();
//            is = twitterStream.getSiteStream(true, new int[]{4933401, 6358482});
//            InputStreamReader isr = new InputStreamReader(is);
//            BufferedReader br = new BufferedReader(isr);
//            String line;
//            while (null != (line = br.readLine())) {
//                System.out.println(line);
//            }
//        }
//    }

    private synchronized void waitForStatus() {
        try {
            this.wait(5000);
            System.out.println("notified.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    List<Object[]> received = new ArrayList<Object[]>(3);

    private synchronized void notifyResponse() {
        this.notify();
    }

    public void onStatus(int forUser, Status status) {
        received.add(new Object[]{"onstatus", forUser, status});
        assertNotNull(DataObjectFactory.getRawJSON(status));
        notifyResponse();
    }

    public void onDeletionNotice(int forUser, StatusDeletionNotice statusDeletionNotice) {
        received.add(new Object[]{TwitterMethod.DESTROY_STATUS, forUser, statusDeletionNotice});
        notifyResponse();
    }

    public void onDeletionNotice(int forUser, int directMessageId, int userId) {
        received.add(new Object[]{TwitterMethod.DESTROY_DIRECT_MESSAGE, forUser, directMessageId, userId});
        notifyResponse();
    }

    public void onFriendList(int forUser, int[] friendIds) {
        received.add(new Object[]{"onfriendlist", forUser, friendIds});
        notifyResponse();
    }

    public void onFavorite(int forUser, User source, User target, Status favoritedStatus) {
        received.add(new Object[]{TwitterMethod.CREATE_FAVORITE, forUser, source, target, favoritedStatus});
        assertNotNull(DataObjectFactory.getRawJSON(source));
        assertNotNull(DataObjectFactory.getRawJSON(target));
        assertNotNull(DataObjectFactory.getRawJSON(favoritedStatus));
        notifyResponse();
    }

    public void onUnfavorite(int forUser, User source, User target, Status unfavoritedStatus) {
        received.add(new Object[]{TwitterMethod.DESTROY_FAVORITE, forUser, source, target, unfavoritedStatus});
        assertNotNull(DataObjectFactory.getRawJSON(source));
        assertNotNull(DataObjectFactory.getRawJSON(target));
        assertNotNull(DataObjectFactory.getRawJSON(unfavoritedStatus));
        notifyResponse();
    }

    public void onFollow(int forUser, User source, User followedUser) {
        received.add(new Object[]{TwitterMethod.CREATE_FRIENDSHIP, forUser, source, followedUser});
        assertNotNull(DataObjectFactory.getRawJSON(source));
        assertNotNull(DataObjectFactory.getRawJSON(followedUser));
        notifyResponse();
    }

    public void onUnfollow(int forUser, User source, User followedUser) {
        received.add(new Object[]{TwitterMethod.DESTROY_FRIENDSHIP, forUser, source, followedUser});
        assertNotNull(DataObjectFactory.getRawJSON(source));
        assertNotNull(DataObjectFactory.getRawJSON(followedUser));
        notifyResponse();
    }

    public void onDirectMessage(int forUser, DirectMessage directMessage) {
        received.add(new Object[]{TwitterMethod.SEND_DIRECT_MESSAGE, forUser, directMessage});
        assertNotNull(DataObjectFactory.getRawJSON(directMessage));
        notifyResponse();
    }

    public void onUserListMemberAddition(int forUser, User addedMember, User listOwner, UserList list) {
        received.add(new Object[]{TwitterMethod.ADD_LIST_MEMBER, forUser, addedMember, listOwner, list});
        assertNotNull(DataObjectFactory.getRawJSON(addedMember));
        assertNotNull(DataObjectFactory.getRawJSON(listOwner));
        assertNotNull(DataObjectFactory.getRawJSON(list));
        notifyResponse();
    }

    public void onUserListMemberDeletion(int forUser, User deletedMember, User listOwner, UserList list) {
        received.add(new Object[]{TwitterMethod.DELETE_LIST_MEMBER, forUser, deletedMember, listOwner, list});
        assertNotNull(DataObjectFactory.getRawJSON(deletedMember));
        assertNotNull(DataObjectFactory.getRawJSON(listOwner));
        assertNotNull(DataObjectFactory.getRawJSON(list));
        notifyResponse();
    }

    public void onUserListSubscription(int forUser, User subscriber, User listOwner, UserList list) {
        received.add(new Object[]{TwitterMethod.SUBSCRIBE_LIST, forUser, subscriber, listOwner, list});
        assertNotNull(DataObjectFactory.getRawJSON(subscriber));
        assertNotNull(DataObjectFactory.getRawJSON(listOwner));
        assertNotNull(DataObjectFactory.getRawJSON(list));
        notifyResponse();
    }

    public void onUserListUnsubscription(int forUser, User subscriber, User listOwner, UserList list) {
        received.add(new Object[]{TwitterMethod.UNSUBSCRIBE_LIST, forUser, subscriber, listOwner, list});
        assertNotNull(DataObjectFactory.getRawJSON(subscriber));
        assertNotNull(DataObjectFactory.getRawJSON(listOwner));
        assertNotNull(DataObjectFactory.getRawJSON(list));
        notifyResponse();
    }

    public void onUserListCreation(int forUser, User listOwner, UserList list) {
        received.add(new Object[]{TwitterMethod.CREATE_USER_LIST, forUser, listOwner, list});
        assertNotNull(DataObjectFactory.getRawJSON(listOwner));
        assertNotNull(DataObjectFactory.getRawJSON(list));
        notifyResponse();
    }

    public void onUserListUpdate(int forUser, User listOwner, UserList list) {
        received.add(new Object[]{TwitterMethod.UPDATE_USER_LIST, forUser, listOwner, list});
        assertNotNull(DataObjectFactory.getRawJSON(listOwner));
        assertNotNull(DataObjectFactory.getRawJSON(list));
        notifyResponse();
    }

    public void onUserListDeletion(int forUser, User listOwner, UserList list) {
        received.add(new Object[]{TwitterMethod.DESTROY_USER_LIST, forUser, listOwner, list});
        notifyResponse();
        assertNotNull(DataObjectFactory.getRawJSON(listOwner));
        assertNotNull(DataObjectFactory.getRawJSON(list));
    }

    public void onUserProfileUpdate(int forUser, User updatedUser) {
        received.add(new Object[]{TwitterMethod.UPDATE_PROFILE, forUser, updatedUser});
        assertNotNull(DataObjectFactory.getRawJSON(updatedUser));
        notifyResponse();
    }

    public void onBlock(int forUser, User source, User blockedUser) {
        received.add(new Object[]{TwitterMethod.CREATE_BLOCK, forUser, source, blockedUser});
        assertNotNull(DataObjectFactory.getRawJSON(source));
        assertNotNull(DataObjectFactory.getRawJSON(blockedUser));
        notifyResponse();
    }

    public void onUnblock(int forUser, User source, User unblockedUser) {
        received.add(new Object[]{TwitterMethod.DESTROY_BLOCK, forUser, source, unblockedUser});
        assertNotNull(DataObjectFactory.getRawJSON(source));
        assertNotNull(DataObjectFactory.getRawJSON(unblockedUser));
        notifyResponse();
    }

    public void onException(Exception ex) {
        received.add(new Object[]{ex});
        ex.printStackTrace();
        notifyResponse();
    }
}
