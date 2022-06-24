package com.revature.nutritioknights.fight;

import com.revature.nutritioknights.avatar.AvatarService;
import com.revature.nutritioknights.fight.dtos.requests.NewFightRequest;
import com.revature.nutritioknights.level.LevelService;
import com.revature.nutritioknights.monster.MonsterService;
import com.revature.nutritioknights.util.annotations.Inject;
import com.revature.nutritioknights.util.custom_exceptions.InvalidRequestException;
import com.revature.nutritioknights.util.custom_exceptions.ResourceConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class FightService {

    @Inject
    private final FightRepository fightRepository;
    private final AvatarService avatarService;
    private final MonsterService  monsterService;
    private final LevelService levelService;

    @Inject
    @Autowired
    public FightService(FightRepository fightRepository, AvatarService avatarService, MonsterService monsterService, LevelService levelService) {
        this.fightRepository = fightRepository;
        this.avatarService = avatarService;
        this.monsterService = monsterService;
        this.levelService = levelService;
    }

    public String newFight(NewFightRequest request){
        Fight newFight = new Fight(request);

        if(hasActiveFight(request.getUsername()))throw new ResourceConflictException("You cannot have more than 1 fight");

        newFight.setId(UUID.randomUUID().toString());

        newFight.setFight_monster_hp(monsterService.getMonsterByID(request.getMonster_id()).getMonster_max_hp());
        newFight.setFight_avatar_hp(levelService.getByLevel(avatarService.getByUsername(request.getUsername()).getLevel()).getMax_hp());

        newFight.setLastChecked(new Date().getTime()/(1000*60*60*24));

        //new Monster no attack
        newFight.setMonster_hits(0);

        newFight.setActive(true);

        fightRepository.save(newFight);

        return newFight.getId();

    }

    private boolean hasActiveFight(String username){
        return fightRepository.getAllActivityByUsername(username).contains(true);
    }

    public Optional<Fight> getCurrentFightByUsername(String username) {
        return fightRepository.getActiveByUsername(username);
    }

    public Optional<Fight> getCurrentFightById(String id) {
        return fightRepository.getActiveById(id);
    }

    public Fight update(Fight currentFight) {
        return fightRepository.save(currentFight);
    }

    public Optional<Fight> updatedFight(String username){
        Fight curFight = new Fight();
        try{
            curFight = getCurrentFightByUsername(username).get();
            long today  = new Date().getTime()/(1000*60*60*24);
            if(today != curFight.getLastChecked()){
                curFight.setMonster_hits((int)(curFight.getMonster_hits() + today - curFight.getLastChecked()));
                curFight.setLastChecked(today);
                return Optional.of(fightRepository.save(curFight));
            }

            }catch (NoSuchElementException e) {throw new InvalidRequestException("No fights");}

        return Optional.of(curFight);
    }
}
