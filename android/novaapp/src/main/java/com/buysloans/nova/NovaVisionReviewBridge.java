package com.buysloans.nova;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

final class NovaVisionReviewBridge {
    static final class Snapshot {
        final ArrayList<Uri> photos;
        final JSONObject assessment;
        Snapshot(ArrayList<Uri> photos, JSONObject assessment) {
            this.photos = photos;
            this.assessment = assessment;
        }
    }

    private static final ArrayList<Uri> PHOTOS = new ArrayList<>();
    private static JSONObject assessment;

    private NovaVisionReviewBridge() {}

    static synchronized int capture(NovaVisionActivity activity) {
        try {
            Field photosField = NovaVisionActivity.class.getDeclaredField("sessionUris");
            Field assessmentField = NovaVisionActivity.class.getDeclaredField("lastAssessment");
            photosField.setAccessible(true);
            assessmentField.setAccessible(true);
            Object photoValue = photosField.get(activity);
            Object assessmentValue = assessmentField.get(activity);
            if (!(photoValue instanceof List) || !(assessmentValue instanceof JSONObject)) return 0;

            ArrayList<Uri> nextPhotos = new ArrayList<>();
            for (Object item : (List<?>) photoValue) if (item instanceof Uri) nextPhotos.add((Uri) item);
            JSONObject nextAssessment = new JSONObject(assessmentValue.toString());
            JSONArray regions = nextAssessment.optJSONArray("damage_regions");
            if (nextPhotos.isEmpty() || regions == null || regions.length() == 0) return 0;

            PHOTOS.clear();
            PHOTOS.addAll(nextPhotos);
            assessment = nextAssessment;
            return regions.length();
        } catch (Exception ignored) {
            return 0;
        }
    }

    static synchronized Snapshot snapshot() {
        if (assessment == null || PHOTOS.isEmpty()) return null;
        try {
            return new Snapshot(new ArrayList<>(PHOTOS), new JSONObject(assessment.toString()));
        } catch (Exception ignored) {
            return null;
        }
    }

    static synchronized void clear() {
        PHOTOS.clear();
        assessment = null;
    }
}
