package tn.esprit.tic.civiAgora.controller.saas;

import tn.esprit.tic.civiAgora.auth.AuthenticationService;
import tn.esprit.tic.civiAgora.dto.usersDto.BackOfficeSAASUserDto;
import tn.esprit.tic.civiAgora.dto.usersDto.UserProfileDto;
import tn.esprit.tic.civiAgora.service.EmailService;
import tn.esprit.tic.civiAgora.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/saas/users")
public class SAASController {

    @Autowired
    private UserService service;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private EmailService emailService;







// Get the users for the saas section
    @GetMapping
    public ResponseEntity<List<BackOfficeSAASUserDto>> getBackOfficeSaasUsers() {
        List<BackOfficeSAASUserDto> users = service.getBackOfficeSaasUsers();
        return ResponseEntity.ok(users);
    }

//
//
//    @PostMapping
//    public ResponseEntity<?> addUser(@RequestBody RegisterRequest request) {
//        try {
//            authenticationService.register(request);
//            return ResponseEntity.ok().build();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    @PatchMapping
//    public ResponseEntity<User> updateUser(@RequestBody User user) throws Exception {
//        return ResponseEntity.ok(service.updateUser(user));
//    }
//
//    @GetMapping("{id}")
//    public ResponseEntity<User> getUserById(@PathVariable Integer id) throws Exception, Exception {
//        User user = service.getUserById(id);
//        return ResponseEntity.ok(user);
//    }
//
//    // method to get a user by email
//    @GetMapping("email/{email}")
//    public ResponseEntity<User> getUserByemail(@PathVariable("email") String email) {
//        try {
//            return new ResponseEntity<>(service.getUserByEmail(email), HttpStatus.OK);
//        } catch (Exception e) {
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
//    }
//
//
//    @DeleteMapping("{id}")
//    public ResponseEntity<?> deleteUser(@PathVariable Integer id) {
//        try {
//            service.deleteUser(id);
//            return new ResponseEntity<>(HttpStatus.OK);
//        } catch (Exception e) {
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
//    }
//    @GetMapping("archived/{archived}")
//    public ResponseEntity<List<User>> getAllUsersByArchived(@PathVariable("archived") Boolean archived) {
//        List<User> users = service.getUsersByArchived(archived);
//        return ResponseEntity.ok(users);
//    }
//
//    @PatchMapping("archive/{userId}/{archived}")
//    public ResponseEntity<?> setArchive(@PathVariable("userId") Integer userId , @PathVariable("archived") Boolean archived) throws Exception {
//        return ResponseEntity.ok(service.setArchive(userId,archived));
//    }



//    @GetMapping("checkPassRecText/{email}/{recText}")
//    public ResponseEntity<?> checkRecText(@PathVariable("email") String email, @PathVariable("recText") String recText) {
//        try {
//            User user = service.getUserByEmail(email);
//            return new ResponseEntity<>(HttpStatus.OK);
//        } catch (Exception e) {
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
//
//
//    }
//
//    @PatchMapping("resetPass/{email}/{newPass}")
//    public ResponseEntity<?>resetPass(@PathVariable("email") String email, @PathVariable("newPass")String newPass){
//        try{
//            User user = service.getUserByEmail(email);
//            user.setPassword(newPass);
//            service.updateUser(user);
//            return new ResponseEntity<>(HttpStatus.OK);
//        } catch (Exception e) {
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }//   }






}
