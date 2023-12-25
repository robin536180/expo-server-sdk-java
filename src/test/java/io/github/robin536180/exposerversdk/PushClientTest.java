package io.github.robin536180.exposerversdk;

import io.github.robin536180.exposerversdk.enums.Priority;
import io.github.robin536180.exposerversdk.enums.ReceiptError;
import io.github.robin536180.exposerversdk.enums.Status;
import io.github.robin536180.exposerversdk.helpers.PushServerResolver;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PushClientTest {

    @Test
    public void apiBaseUrlIsOverridable() throws MalformedURLException, PushClientException {
        PushClient client = new PushClient();
        URL apiUrl = client.getBaseApiUrl();
        assertEquals(apiUrl, client.getBaseApiUrl());
        URL mockBaseApiUrl = new URL("http://example.com/");
        client.setBaseApiUrl(mockBaseApiUrl);
        assertEquals(mockBaseApiUrl, client.getBaseApiUrl());
    }

    @Test
    public void chunkListsOfPushNotificationMessages() throws PushClientException {
        PushClient client = new PushClient();
        List<ExpoPushMessage> messages = new ArrayList<>(Collections.nCopies(999, new ExpoPushMessage("?")));
        List<List<ExpoPushMessage>> chunks = client.chunkPushNotifications(messages);
        long totalMessageCount = 0;
        for (List<ExpoPushMessage> chunk : chunks) {
            totalMessageCount += chunk.size();
        }
        assertEquals(totalMessageCount, messages.size());
    }

    @Test
    public void canChunkSmallListsOfPushNotificationMessages() throws PushClientException {
        PushClient client = new PushClient();
        List<ExpoPushMessage> messages = new ArrayList<>(Collections.nCopies(10, new ExpoPushMessage("?")));
        List<List<ExpoPushMessage>> chunks = client.chunkPushNotifications(messages);
        assertEquals(1, chunks.size());
        assertEquals(10, chunks.get(0).size());
    }

    @Test
    public void canChunkSinglePushNotificationMessageWithListsOfRecipients() throws PushClientException {
        int messagesLength = 999;
        PushClient client = new PushClient();
        List<ExpoPushMessage> messages = new ArrayList<>();
        messages.add(new ExpoPushMessage(Collections.nCopies(messagesLength, "?")));
        List<List<ExpoPushMessage>> chunks = client.chunkPushNotifications(messages);
        chunks.stream().forEach(
                (c) -> assertEquals(1, c.size())
        );
        long totalMessageCount = _countAndValidateMessages(chunks);
        assertEquals(totalMessageCount, messagesLength);
    }

    @Test
    public void canChunkSinglePushNotificatoinMessageWithSmallListsOfRecipients() throws PushClientException {
        int messagesLength = 10;
        PushClientCustomData<ExpoPushMessageCustomData<Integer>> client = new PushClientCustomData<>();
        List<ExpoPushMessageCustomData<Integer>> messages = new ArrayList<>();
        messages.add(new ExpoPushMessageCustomData<Integer>(Collections.nCopies(messagesLength, "?")));
        List<List<ExpoPushMessageCustomData<Integer>>> chunks = client.chunkPushNotifications(messages);
        assertEquals(1, chunks.size());
        assertEquals(1, chunks.get(0).size());
        assertEquals(messagesLength, chunks.get(0).get(0).getTo().size());
    }

    @Test
    public void chunksPushNotificationMessagesMixedWithListsOfRecipientsAndSingleRecipient() throws PushClientException {
        PushClient client = new PushClient();
        List<ExpoPushMessage> messages = new ArrayList<>();
        messages.add(new ExpoPushMessage(Collections.nCopies(888, "?")));
        messages.addAll(Collections.nCopies(999, new ExpoPushMessage("?")));
        messages.add(new ExpoPushMessage(Collections.nCopies(90, "?")));
        messages.addAll(Collections.nCopies(10, new ExpoPushMessage("?")));

        List<List<ExpoPushMessage>> chunks = client.chunkPushNotifications(messages);
        long totalMessageCount = _countAndValidateMessages(chunks);
        assertEquals(888 + 999 + 90 + 10, totalMessageCount);
    }

    @Test
    public void isExponentPushTokenCanValidateTokens() {
        String validToken1 = "ExpoPushToken[xxxxxxxxxxxxxxxxxxxxxx]";
        assertEquals(true, PushClient.isExponentPushToken(validToken1), "Test that \"ExpoPusToken[\" is a valid token");
        String validToken2 = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]";
        assertEquals(true, PushClient.isExponentPushToken(validToken2), "Test that \"ExponentPushToken[\" is a valid token");
        String validToken3 = "F5741A13-BCDA-434B-A316-5DC0E6FFA94F";
        assertEquals(true, PushClient.isExponentPushToken(validToken3), "Test that \"F5741A13-BCDA-434B-A316-5DC0E6FFA94F\" is a valid token");

        String inValidToken1 = "ExponentPushToken xxxxxxxxxxxxxxxxxxxxxx";
        assertEquals(false, PushClient.isExponentPushToken(inValidToken1), "Stripped []");
        String inValidToken2 = "ExpoXXXXXushToken[xxxxxxxxxxxxxxxxxxxxxx]";
        assertEquals(false, PushClient.isExponentPushToken(inValidToken1), "Mangeled prefix");
        String inValidToken3 = "ExponentPushToken[]";
        assertEquals(false, PushClient.isExponentPushToken(inValidToken1), "Empty key field");
    }

    @Test
    public void chunkPushNotificationReceiptIdsCanChunkCorrectly() throws PushClientException {
        PushClient client = new PushClient();
        List<String> recieptIds = new ArrayList<>(Collections.nCopies(60, "F5741A13-BCDA-434B-A316-5DC0E6FFA94F"));
        List<List<String>> chunks = client.chunkPushNotificationReceiptIds(recieptIds);
        int totalReceiptIdCount = 0;
        for (List<String> chunk : chunks) {
            totalReceiptIdCount += chunk.size();
        }
        assertEquals(recieptIds.size(), totalReceiptIdCount);
    }

    @Test
    public void chunkTwoMessagesAndOneAdditionalMessageWithNoRecipient() throws PushClientException {
        PushClient client = new PushClient();

        List<ExpoPushMessage> messages = new ArrayList<>(Collections.nCopies(2, new ExpoPushMessage("?")));
        messages.add(new ExpoPushMessage(new ArrayList<>()));
        List<List<ExpoPushMessage>> chunks = client.chunkPushNotifications(messages);
        assertEquals(1, chunks.size());
        long totalMessageCount = _countAndValidateMessages(chunks);
        assertEquals(2, totalMessageCount);
    }

    @Test
    public void chunkOneMessageWith100Recipients() throws PushClientException {
        PushClient client = new PushClient();
        List<ExpoPushMessage> messages = new ArrayList<>();
        messages.add(new ExpoPushMessage(Collections.nCopies(100, "?")));
        List<List<ExpoPushMessage>> chunks = client.chunkPushNotifications(messages);
        assertEquals(1, chunks.size());
        assertEquals(1, chunks.get(0).size());
        List<ExpoPushMessage> epmList = chunks.get(0);
        ExpoPushMessage epm = epmList.get(0);
        int numberOfRecipients = epm.getTo().size();
        assertEquals(100, numberOfRecipients);
    }

    @Test
    // TODO: This test only works because we assume that max-chunk size is 100
    public void chunkOneMessageWith101Recipients() throws PushClientException {
        PushClient client = new PushClient();
        List<ExpoPushMessage> messages = new ArrayList<>();
        messages.add(new ExpoPushMessage(Collections.nCopies(101, "?")));
        List<List<ExpoPushMessage>> chunks = client.chunkPushNotifications(messages);
        assertEquals(2, chunks.size());
        assertEquals(1, chunks.get(0).size());
        assertEquals(1, chunks.get(1).size());
        long totalMessageCount = _countAndValidateMessages(chunks);
        assertEquals(101, totalMessageCount);
    }

    @Test
    // TODO: This test only works because we assume that max-chunk size is 100
    public void chunkOneMessageWith99RecipientsAndTwoAdditionalMessages() throws PushClientException {
        PushClient client = new PushClient();
        List<ExpoPushMessage> messages = new ArrayList<>();
        messages.add(new ExpoPushMessage(Collections.nCopies(99, "?")));
        messages.add(new ExpoPushMessage("?"));
        messages.add(new ExpoPushMessage("?"));

        List<List<ExpoPushMessage>> chunks = client.chunkPushNotifications(messages);
        assertEquals(2, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(1, chunks.get(1).size());
        long totalMessageCount = _countAndValidateMessages(chunks);
        assertEquals(99 + 2, totalMessageCount);
    }

    @Test
    // TODO: This test only works because we assume that max-chunk size is 100
    public void chunkOneMessageWith100RecipientsAndTwoAdditionalMessages() throws PushClientException {
        PushClient client = new PushClient();
        List<ExpoPushMessage> messages = new ArrayList<>();
        messages.add(new ExpoPushMessage(Collections.nCopies(100, "?")));
        messages.add(new ExpoPushMessage("?"));
        messages.add(new ExpoPushMessage("?"));

        List<List<ExpoPushMessage>> chunks = client.chunkPushNotifications(messages);
        assertEquals(2, chunks.size());
        assertEquals(1, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        long totalMessageCount = _countAndValidateMessages(chunks);
        assertEquals(100 + 2, totalMessageCount);
    }


    @Test
    // TODO: This test only works because we assume that max-chunk size is 100
    public void chunk99MessagesAndOneAdditionalMessageWithTwoRecipients() throws PushClientException {
        PushClient client = new PushClient();
        List<ExpoPushMessage> messages = new ArrayList<>((Collections.nCopies(99, new ExpoPushMessage("?"))));
        messages.add(new ExpoPushMessage(Collections.nCopies(2, "?")));

        List<List<ExpoPushMessage>> chunks = client.chunkPushNotifications(messages);
        assertEquals(2, chunks.size());
        assertEquals(100, chunks.get(0).size());
        assertEquals(1, chunks.get(1).size());
        long totalMessageCount = _countAndValidateMessages(chunks);
        assertEquals(99 + 2, totalMessageCount);
    }

    @Test
    public void chunkNoMessage() throws PushClientException {
        PushClient client = new PushClient();
        List<ExpoPushMessage> messages = new ArrayList<>();
        List<List<ExpoPushMessage>> chunks = client.chunkPushNotifications(messages);
        assertEquals(0, chunks.size());
    }

    @Test
    public void chunkSingleMessageWithNoRecipient() throws PushClientException {
        PushClient client = new PushClient();
        List<ExpoPushMessage> messages = new ArrayList();
        messages.add(new ExpoPushMessage());
        List<List<ExpoPushMessage>> chunks = client.chunkPushNotifications(messages);
        assertEquals(0, chunks.size());
    }

    private long _countAndValidateMessages(List<List<ExpoPushMessage>> chunks) throws PushClientException {
        long totalMessageCount = 0;
        for (List<ExpoPushMessage> chunk : chunks) {
            PushClient client = new PushClient();
            long chunkMessagesCount = client._getActualMessagesCount(chunk);
            assert (chunkMessagesCount <= client.PUSH_NOTIFICATION_CHUNK_LIMIT);
            totalMessageCount += chunkMessagesCount;
        }
        return totalMessageCount;

    }

    @Test
    public void getOneReceipt() throws InterruptedException, ExecutionException, PushClientException {
        final String SOURCE_JSON = "{" +
                "    \"data\": " +
                "    {" +
                "        \"2011eb6d-d4d3-440c-a93c-37ac4b51ea09\": { " +
                "            \"status\":\"error\"," +
                "            \"message\":\"The Apple Push Notification service ...  this error means.\"," +
                "            \"details\":{" +
                "                \"apns\":{" +
                "                    \"reason\":\"PayloadTooLarge\", " +
                "                    \"statusCode\":413" +
                "                }," +
                "                \"error\":\"MessageTooBig\"," +
                "                \"sentAt\":1586353449" +
                "            }," +
                "            \"__debug\": {}" +
                "        }" +
                "    }" +
                "}";


        CompletableFuture<String> mockResponseFuture = new CompletableFuture<>().completedFuture(SOURCE_JSON);

        PushServerResolver pushServerResolverMock = mock(PushServerResolver.class);
        when(pushServerResolverMock.postAsync(any(), any())).thenReturn(mockResponseFuture);

        PushClient client = new PushClient();
        client.pushServerResolver = pushServerResolverMock;

        List<CompletableFuture<List<ExpoPushReceipt>>> messageRepliesFutures = new ArrayList<>();
        List<List<String>> receiptIdChunks = client.chunkPushNotificationReceiptIds(Arrays.asList("2011eb6d-d4d3-440c-a93c-37ac4b51ea09"));
        for (List<String> chunk : receiptIdChunks) {
            messageRepliesFutures.add(client.getPushNotificationReceiptsAsync(chunk));
        }

        ExpoPushReceipt receipt = messageRepliesFutures.get(0).get().get(0);
        assertEquals("2011eb6d-d4d3-440c-a93c-37ac4b51ea09", receipt.getId());
        assertEquals(Status.ERROR, receipt.getStatus());
        assertTrue(receipt.getMessage().startsWith("The Apple Push"));
        assertTrue(receipt.getMessage().endsWith("this error means."));
        assertEquals(ReceiptError.MESSAGETOOBIG, receipt.getDetails().getError());
    }

    @Test
    public void sendPushNotificationsAsyncThrowsExceptionWithAllFailedMessages() throws PushClientException {
        PushServerResolver pushServerResolverMock = mock(PushServerResolver.class);
        when(pushServerResolverMock.postAsync(any(), any())).thenThrow(new CompletionException(new Exception("Exception!")));

        PushClient client = new PushClient();
        client.pushServerResolver = pushServerResolverMock;

        List<ExpoPushMessage> messages = new ArrayList<>(Arrays.asList(new ExpoPushMessage("Recipient 1")));
        List<List<ExpoPushMessage>> messageChunks = client.chunkPushNotifications(messages);

        PushNotificationException pushNotificationException = null;

        try {
            for (List<ExpoPushMessage> chunk : messageChunks) {
                client.sendPushNotificationsAsync(chunk);
            }
        } catch (PushNotificationException e) {
            pushNotificationException = e;
        }

        assertNotNull(pushNotificationException);
        assertEquals(pushNotificationException.messages.size(), 1);
        assertEquals(pushNotificationException.messages.get(0).getTo().get(0), "Recipient 1");
    }

    @Test
    public void sendPushNotificationsReceiptAsyncThrowsExceptionWithAllFailedMessages() throws PushClientException {
        PushServerResolver pushServerResolverMock = mock(PushServerResolver.class);
        when(pushServerResolverMock.postAsync(any(), any())).thenThrow(new CompletionException(new Exception("Exception!")));

        PushClient client = new PushClient();
        client.pushServerResolver = pushServerResolverMock;

        List<List<String>> receiptIdChunks = client.chunkPushNotificationReceiptIds(Arrays.asList("2011eb6d-d4d3-440c-a93c-37ac4b51ea09"));

        PushNotificationReceiptsException pushNotificationReceiptsException = null;

        try {
            for (List<String> chunk : receiptIdChunks) {
                client.getPushNotificationReceiptsAsync(chunk);
            }
        } catch (PushNotificationReceiptsException e) {
            pushNotificationReceiptsException = e;
        }

        assertNotNull(pushNotificationReceiptsException);
        assertEquals(pushNotificationReceiptsException.ids.size(), 1);
        assertEquals(pushNotificationReceiptsException.ids.get(0), "2011eb6d-d4d3-440c-a93c-37ac4b51ea09");
    }

    @Test
    public void sendPushNotificationsAsyncThrowsErrorsException() throws InterruptedException, PushClientException {
        final String RESPONSE_JSON = "{" +
                "    \"errors\": [" +
                "        { " +
                "            \"code\":\"PUSH_TOO_MANY_EXPERIENCE_IDS\"," +
                "            \"message\":\"All...tokens.\"," +
                "            \"details\":{" +
                "                \"@user1/project1\":[" +
                "                    \"ExponentPushToken[AAAAAAAAAAABBBBBBBBBBB]\"" +
                "                ]," +
                "                \"@user2/project1\":[" +
                "                    \"ExponentPushToken[AAAAAAAAAAABBBBBBBBBBB]\"" +
                "                ]" +
                "            }" +
                "        }" +
                "    ]" +
                "}";
        CompletableFuture<String> mockResponseFuture = CompletableFuture.completedFuture(RESPONSE_JSON);

        PushServerResolver pushServerResolverMock = mock(PushServerResolver.class);
        when(pushServerResolverMock.postAsync(any(), any())).thenReturn(mockResponseFuture);

        PushClient client = new PushClient();
        client.pushServerResolver = pushServerResolverMock;

        ExpoPushMessage message = new ExpoPushMessage();
        message.setTo(Arrays.asList("ExponentPushToken[AAAAAAAAAAABBBBBBBBBBB]", "ExponentPushToken[AAAAAAAAAAABBBBBBBBBBB]"));
        message.setPriority(Priority.NORMAL);
        message.setChannelId("default");
        message.setSound(new ExpoMessageSound("default"));

        List<ExpoPushMessage> messages = new ArrayList<>(Arrays.asList(message));

        CompletableFuture<List<ExpoPushTicket>> future = client.sendPushNotificationsAsync(messages);
        try {
            future.get();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof PushNotificationException);
            PushNotificationException pushNotificationException = (PushNotificationException)e.getCause();

            assertTrue(pushNotificationException.exception instanceof PushNotificationErrorsException);
            PushNotificationErrorsException errorsException = (PushNotificationErrorsException)pushNotificationException.exception;
            assertEquals(1, errorsException.errors.size());
            assertEquals(0, errorsException.data.size());

            ExpoPushError error = errorsException.errors.get(0);
            assertEquals("PUSH_TOO_MANY_EXPERIENCE_IDS", error.getCode());
            assertEquals("All...tokens.", error.getMessage());
        }
    }

    @Test
    public void sendPushNotificationsAsyncThrowsErrorsExceptionWithData() throws InterruptedException, PushClientException {
        final String RESPONSE_JSON = "{" +
                "    \"data\": [" +
                "        { " +
                "            \"status\":\"error\"," +
                "            \"message\":\"ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx] is not a registered push notification recipient\"," +
                "            \"details\": {" +
                "                \"error\": \"DeviceNotRegistered\"" +
                "            }" +
                "        }" +
                "    ]," +
                "    \"errors\": [" +
                "        { " +
                "            \"code\":\"PUSH_TOO_MANY_EXPERIENCE_IDS\"," +
                "            \"message\":\"All...tokens.\"," +
                "            \"details\":{" +
                "                \"@user1/project1\":[" +
                "                    \"ExponentPushToken[AAAAAAAAAAABBBBBBBBBBB]\"" +
                "                ]," +
                "                \"@user2/project1\":[" +
                "                    \"ExponentPushToken[AAAAAAAAAAABBBBBBBBBBB]\"" +
                "                ]" +
                "            }" +
                "        }" +
                "    ]" +
                "}";
        CompletableFuture<String> mockResponseFuture = CompletableFuture.completedFuture(RESPONSE_JSON);

        PushServerResolver pushServerResolverMock = mock(PushServerResolver.class);
        when(pushServerResolverMock.postAsync(any(), any())).thenReturn(mockResponseFuture);

        PushClient client = new PushClient();
        client.pushServerResolver = pushServerResolverMock;

        ExpoPushMessage message = new ExpoPushMessage();
        message.setTo(Arrays.asList("ExponentPushToken[AAAAAAAAAAABBBBBBBBBBB]", "ExponentPushToken[AAAAAAAAAAABBBBBBBBBBB]"));
        message.setPriority(Priority.NORMAL);
        message.setChannelId("default");
        message.setSound(new ExpoMessageSound("default"));

        List<ExpoPushMessage> messages = new ArrayList<>(Arrays.asList(message));

        CompletableFuture<List<ExpoPushTicket>> future = client.sendPushNotificationsAsync(messages);
        try {
            future.get();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof PushNotificationException);
            PushNotificationException pushNotificationException = (PushNotificationException)e.getCause();

            assertTrue(pushNotificationException.exception instanceof PushNotificationErrorsException);
            PushNotificationErrorsException errorsException = (PushNotificationErrorsException)pushNotificationException.exception;
            assertEquals(1, errorsException.errors.size());
            assertEquals(1, errorsException.data.size());

            ExpoPushError error = errorsException.errors.get(0);
            assertEquals("PUSH_TOO_MANY_EXPERIENCE_IDS", error.getCode());
            assertEquals("All...tokens.", error.getMessage());

            ExpoPushTicket ticket = errorsException.data.get(0);
            assertEquals(Status.ERROR, ticket.getStatus());
        }
    }

    @Test
    public void getTooManyReceiptsThrowsErrorsException() throws InterruptedException, PushClientException {
        final String RESPONSE_JSON = "{" +
                "    \"errors\": [" +
                "        { " +
                "            \"code\":\"PUSH_TOO_MANY_RECEIPTS\"," +
                "            \"message\":\"Too many receipts.\"" +
                "        }" +
                "    ]" +
                "}";
        CompletableFuture<String> mockResponseFuture = CompletableFuture.completedFuture(RESPONSE_JSON);

        PushServerResolver pushServerResolverMock = mock(PushServerResolver.class);
        when(pushServerResolverMock.postAsync(any(), any())).thenReturn(mockResponseFuture);

        PushClient client = new PushClient();
        client.pushServerResolver = pushServerResolverMock;

        CompletableFuture<List<ExpoPushReceipt>> future = client.getPushNotificationReceiptsAsync(Arrays.asList("1", "2"));
        try {
            future.get();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof PushNotificationReceiptsException);
            PushNotificationReceiptsException pushNotificationException = (PushNotificationReceiptsException)e.getCause();

            assertTrue(pushNotificationException.exception instanceof PushNotificationReceiptsErrorsException);
            PushNotificationReceiptsErrorsException errorsException =
                    (PushNotificationReceiptsErrorsException)pushNotificationException.exception;
            assertEquals(1, errorsException.errors.size());
            assertEquals(0, errorsException.receipts.size());

            ExpoPushError error = errorsException.errors.get(0);
            assertEquals("PUSH_TOO_MANY_RECEIPTS", error.getCode());
            assertEquals("Too many receipts.", error.getMessage());
        }
    }
}
