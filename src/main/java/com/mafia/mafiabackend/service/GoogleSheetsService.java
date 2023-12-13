package com.mafia.mafiabackend.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.InsertDimensionRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.mafia.mafiabackend.dto.SimpleStatisticDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GoogleSheetsService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsService.class);
    private final String spreadsheetId = "1AoHIfdopqB3TcALJ5p5NEOOo34MnFYR-70aMWlfEAPs";
    private static final String APPLICATION_NAME = "Google Sheets API for Vyasma Mafia";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES =
            Collections.singletonList(SheetsScopes.DRIVE_FILE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

    private final Sheets service;

    public GoogleSheetsService() throws GeneralSecurityException, IOException {
        service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = GoogleSheetsService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("sash39av@gmail.com");
    }

    public void sendResultStatToGoogleSheet(List<SimpleStatisticDto> simpleStatistic) {
        InsertDimensionRequest insertDimensionRequest = new InsertDimensionRequest()
                .setRange(new DimensionRange().setDimension("ROWS")
                        .setStartIndex(0)
                        .setEndIndex(10));
        var statisticTable = simpleStatistic.stream()
                .map(it -> List.<Object>of(
                        prettyDate(it.getGameDate()),
                        it.getGameId(),
                        it.getPlayerName(),
                        it.getSitNumber(),
                        it.getIsRedWin(),
                        it.getIsRed(),
                        it.getBestTurn()
                )).toList();
        try {
            service.spreadsheets()
                    .batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest()
                            .setRequests(List.of(new Request().setInsertDimension(insertDimensionRequest))))
                    .execute();
            //    gameDate,gameId,playerName,redWin,isRed,bestTurn
            service.spreadsheets()
                    .values()
                    .update(spreadsheetId, "A1:G10", new ValueRange().setValues(statisticTable))
                    .setValueInputOption("USER_ENTERED")
                    .execute();
        } catch (IOException e) {
            logger.error("Error on writing in google table", e);
        }

    }

    private String prettyDate(Instant instant) {
        ZoneId zone = ZoneId.of("UTC+3");
        Locale ru = Locale.forLanguageTag("ru");
        return instant.atZone(zone).toLocalDateTime()
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT).withLocale(ru));
    }
}
