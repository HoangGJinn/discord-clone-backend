package com.discordclone.backend.service.search;

import com.discordclone.backend.dto.response.SearchResponse;

import java.util.List;

/**
 * Service interface cho các chức năng tìm kiếm
 */
public interface SearchService {

    /**
     * Tìm kiếm tổng hợp - tìm trong servers, channels, categories, members
     *
     * @param keyword  Từ khóa tìm kiếm
     * @param serverId ID server (optional - để lọc channels, categories, members)
     * @return Kết quả tìm kiếm tổng hợp
     */
    SearchResponse searchAll(String keyword, Long serverId);

    /**
     * Tìm kiếm servers theo tên hoặc mô tả
     *
     * @param keyword Từ khóa tìm kiếm
     * @return Danh sách server tìm được
     */
    List<SearchResponse.ServerSearchResult> searchServers(String keyword);

    /**
     * Tìm kiếm channels theo tên
     *
     * @param keyword  Từ khóa tìm kiếm
     * @param serverId ID server (optional - để lọc trong server cụ thể)
     * @return Danh sách channel tìm được
     */
    List<SearchResponse.ChannelSearchResult> searchChannels(String keyword, Long serverId);

    /**
     * Tìm kiếm categories theo tên
     *
     * @param keyword  Từ khóa tìm kiếm
     * @param serverId ID server (optional - để lọc trong server cụ thể)
     * @return Danh sách category tìm được
     */
    List<SearchResponse.CategorySearchResult> searchCategories(String keyword, Long serverId);

    /**
     * Tìm kiếm members trong server theo displayName, userName, hoặc nickname
     *
     * @param keyword  Từ khóa tìm kiếm
     * @param serverId ID server (bắt buộc)
     * @return Danh sách member tìm được
     */
    List<SearchResponse.MemberSearchResult> searchMembers(String keyword, Long serverId);
}
