package com.jakduk.core.dao;

import com.jakduk.core.common.CoreConst;
import com.jakduk.core.model.db.*;
import com.jakduk.core.model.elasticsearch.ESComment;
import com.jakduk.core.model.elasticsearch.ESGallery;
import com.jakduk.core.model.etc.CommonCount;
import com.jakduk.core.model.etc.SupporterCount;
import com.jakduk.core.model.simple.BoardFreeOnRSS;
import com.jakduk.core.model.simple.BoardFreeSimple;
import com.jakduk.core.model.simple.GalleryOnList;
import com.jakduk.core.model.simple.UserOnHome;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:phjang1983@daum.net">Jang,Pyohwan</a>
 * @company  : http://jakduk.com
 * @date     : 2014. 12. 30.
 * @desc     :
 */

@Repository
public class JakdukDAO {

	@Autowired
	private MongoTemplate mongoTemplate;
	
	/**
	 * 축구단 목록 정렬해서 가져온다.
	 * @param language
	 * @return
	 */
	public List<FootballClub> getFootballClubs(List<ObjectId> ids, String language, CoreConst.NAME_TYPE sortNameType) {

		Query query = new Query();
		query.addCriteria(Criteria.where("names.language").is(language));
		query.addCriteria(Criteria.where("origin.$id").in(ids));
		query.fields().include("active").include("origin").include("names.$");
		//query.with(new Sort(Sort.Direction.DESC, "names.fullName"));
		List<FootballClub> footballClubs = mongoTemplate.find(query, FootballClub.class);

		switch (sortNameType) {
		case fullName:
			footballClubs.sort((f1, f2) -> f1.getNames().get(0).getFullName().compareTo(f2.getNames().get(0).getFullName()));			
			break;
		case shortName:
			footballClubs.sort((f1, f2) -> f1.getNames().get(0).getShortName().compareTo(f2.getNames().get(0).getShortName()));			
			break;			
		}

		return footballClubs;
	}

	// 사진 목록.
	public List<GalleryOnList> findGalleriesById(Direction direction, Integer size, ObjectId galleryId) {
		
		AggregationOperation match1 = Aggregation.match(Criteria.where("status.status").is(CoreConst.GALLERY_STATUS_TYPE.ENABLE.name()));
		AggregationOperation match2 = Aggregation.match(Criteria.where("_id").lt(galleryId));
		AggregationOperation sort = Aggregation.sort(direction, "_id");
		AggregationOperation limit = Aggregation.limit(size);
		
		Aggregation aggregation;
		if (galleryId != null) {
			aggregation = Aggregation.newAggregation(match1, match2, sort, limit);
		} else {
			aggregation = Aggregation.newAggregation(match1, sort, limit);
		}
		
		AggregationResults<GalleryOnList> results = mongoTemplate.aggregate(aggregation, "gallery", GalleryOnList.class);
		
		return results.getMappedResults();
	}

	/**
	 * galleryId에 해당하는 boardFree 가져오기.
	 */
	public List<BoardFreeSimple> getBoardFreeOnGallery(ObjectId galleryId) {
		AggregationOperation unwind = Aggregation.unwind("galleries");
		AggregationOperation match = Aggregation.match(Criteria.where("galleries._id").is(galleryId));
		Aggregation aggregation = Aggregation.newAggregation(unwind, match);
		AggregationResults<BoardFreeSimple> results = mongoTemplate.aggregate(aggregation, "boardFree", BoardFreeSimple.class);

		return results.getMappedResults();
	}

	/**
	 * 사진첩 보기의 앞, 뒤 사진을 가져온다.
	 * @param id
	 * @param direction
	 * @return
	 */
	public Gallery getGalleryById(ObjectId id, Direction direction) {
		Query query = new Query();
		query.addCriteria(Criteria.where("status.status").is(CoreConst.GALLERY_STATUS_TYPE.ENABLE.name()));
		
		if (direction.equals(Sort.Direction.ASC)) {
			query.addCriteria(Criteria.where("_id").gt(id));
		} else if (direction.equals(Sort.Direction.DESC)) {
			query.addCriteria(Criteria.where("_id").lt(id));
		}
		
		query.with(new Sort(direction, "_id"));
		Gallery gallery = mongoTemplate.findOne(query, Gallery.class);
		
		return gallery;
	}

	// 사진의 좋아요 개수 가져오기.
	public Map<String, Integer> findGalleryUsersLikingCount(List<ObjectId> arrId) {
		
		AggregationOperation unwind = Aggregation.unwind("usersLiking");
		AggregationOperation match1 = Aggregation.match(Criteria.where("_id").in(arrId));
		AggregationOperation match2 = Aggregation.match(Criteria.where("status.status").is(CoreConst.GALLERY_STATUS_TYPE.ENABLE.name()));
		AggregationOperation group = Aggregation.group("_id").count().as("count");
		Aggregation aggregation = Aggregation.newAggregation(unwind, match1, match2, group);
		AggregationResults<CommonCount> results = mongoTemplate.aggregate(aggregation, "gallery", CommonCount.class);
		
		List<CommonCount> likingCounts = results.getMappedResults();

		Map<String, Integer> countMap = likingCounts.stream()
				.collect(Collectors.toMap(CommonCount::getId, CommonCount::getCount));

		return countMap;
	}

	// 사진의 싫어요 개수 가져오기.
	public Map<String, Integer> findGalleryUsersDislikingCount(List<ObjectId> arrId) {
		
		AggregationOperation unwind = Aggregation.unwind("usersDisliking");
		AggregationOperation match1 = Aggregation.match(Criteria.where("_id").in(arrId));
		AggregationOperation match2 = Aggregation.match(Criteria.where("status.status").is(CoreConst.GALLERY_STATUS_TYPE.ENABLE.name()));
		AggregationOperation group = Aggregation.group("_id").count().as("count");
		Aggregation aggregation = Aggregation.newAggregation(unwind, match1, match2, group);
		AggregationResults<CommonCount> results = mongoTemplate.aggregate(aggregation, "gallery", CommonCount.class);
		
		List<CommonCount> diskingCount = results.getMappedResults();

		Map<String, Integer> countMap = diskingCount.stream()
				.collect(Collectors.toMap(CommonCount::getId, CommonCount::getCount));

		return countMap;
	}		
	
	public List<SupporterCount> getSupportFCCount(String language) {
		AggregationOperation match = Aggregation.match(Criteria.where("supportFC").exists(true));
		AggregationOperation group = Aggregation.group("supportFC").count().as("count");
		AggregationOperation project = Aggregation.project("count").and("_id").as("supportFC");
		AggregationOperation sort = Aggregation.sort(Direction.DESC, "count");
		Aggregation aggregation = Aggregation.newAggregation(match, group, project, sort);
		
		AggregationResults<SupporterCount> results = mongoTemplate.aggregate(aggregation, "user", SupporterCount.class);
		
		List<SupporterCount> users = results.getMappedResults();
		
		for (SupporterCount supporterCount : users) {
			supporterCount.getSupportFC().getNames().removeIf(fcName -> !fcName.getLanguage().equals(language));
		}
		
		return users;
	}
	
	public List<BoardFreeOnRSS> getRSS() {
		AggregationOperation match = Aggregation.match(Criteria.where("status.delete").ne(CoreConst.BOARD_HISTORY_TYPE.DELETE.name()));
		AggregationOperation sort = Aggregation.sort(Direction.DESC, "_id");
		AggregationOperation limit = Aggregation.limit(CoreConst.RSS_SIZE_ITEM);
		Aggregation aggregation = Aggregation.newAggregation(match, sort, limit);
		
		AggregationResults<BoardFreeOnRSS> results = mongoTemplate.aggregate(aggregation, "boardFree", BoardFreeOnRSS.class);
		
		List<BoardFreeOnRSS> posts = results.getMappedResults();
		
		return posts;
	}	
	
	public List<UserOnHome> getUserOnHome(String language) {
		AggregationOperation sort = Aggregation.sort(Direction.DESC, "_id");
		AggregationOperation limit = Aggregation.limit(CoreConst.HOME_SIZE_LINE_NUMBER);
		Aggregation aggregation = Aggregation.newAggregation(sort, limit);
		
		AggregationResults<UserOnHome> results = mongoTemplate.aggregate(aggregation, "user", UserOnHome.class);
		
		List<UserOnHome> users = results.getMappedResults();
		
		for (UserOnHome user : users) {
			if (user.getSupportFC() != null) {
				user.getSupportFC().getNames().removeIf(fcName -> !fcName.getLanguage().equals(language));
			}
		}
		
		return users;
	}	
	
	public HomeDescription getHomeDescription() {
		
		Query query = new Query();
		query.with(new Sort(Direction.DESC, "priority"));
		HomeDescription homeDescription = mongoTemplate.findOne(query, HomeDescription.class);
		
		return homeDescription;
	}

	// 대회 목록.
	public List<Competition> getCompetitions(List<ObjectId> ids, String language) {

		Query query = new Query();
		query.addCriteria(Criteria.where("_id").in(ids));
		query.addCriteria(Criteria.where("names.language").is(language));
		query.fields().include("code").include("names.$");
		//query.with(new Sort(Sort.Direction.DESC, "date"));
		//query.skip(skip);
		//query.limit(size);

		List<Competition> competitions = mongoTemplate.find(query, Competition.class);

		return competitions;
	}

	public JakduScheduleGroup getJakduScheduleGroupOrderBySeq() {

		Query query = new Query();
		query.with(new Sort(Direction.DESC, "seq"));
		query.limit(1);

		JakduScheduleGroup jakduScheduleGroup = mongoTemplate.findOne(query, JakduScheduleGroup.class);

		return jakduScheduleGroup;
	}

	public List<JakduComment> getJakduComments(String jakduScheduleId, ObjectId commentId) {
		AggregationOperation match1 = Aggregation.match(Criteria.where("jakduScheduleId").is(jakduScheduleId));
		AggregationOperation match2 = Aggregation.match(Criteria.where("_id").gt(commentId));
		AggregationOperation sort = Aggregation.sort(Direction.ASC, "_id");
		AggregationOperation limit = Aggregation.limit(CoreConst.COMMENT_MAX_SIZE);

		Aggregation aggregation;

		if (Objects.nonNull(commentId)) {
			aggregation = Aggregation.newAggregation(match1, match2, sort, limit);
		} else {
			aggregation = Aggregation.newAggregation(match1, sort, limit);
		}

		AggregationResults<JakduComment> results = mongoTemplate.aggregate(aggregation, "jakduComment", JakduComment.class);

		List<JakduComment> comments = results.getMappedResults();

		return comments;
	}
	
}
