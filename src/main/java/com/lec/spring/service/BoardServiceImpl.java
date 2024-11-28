package com.lec.spring.service;

import com.lec.spring.domain.Attachment;
import com.lec.spring.domain.Post;
import com.lec.spring.domain.User;
import com.lec.spring.repository.AttachmentRepository;
import com.lec.spring.repository.PostRepository;
import com.lec.spring.repository.UserRepository;
import com.lec.spring.util.U;
import jakarta.servlet.http.HttpSession;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@Service
public class BoardServiceImpl implements BoardService {

    @Value("${app.upload.path}")
    private String uploadDir;

    @Value("${app.pagination.write_pages}")
    private int WRITE_PAGES;

    @Value("${app.pagination.page_rows}")
    private int PAGE_ROWS;

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final AttachmentRepository attachmentRepository;

    public BoardServiceImpl(SqlSession sqlSession) { // MyBatis 가 생성한 SqlSession 빈(bean) 객체 주입
        postRepository = sqlSession.getMapper(PostRepository.class);
        userRepository = sqlSession.getMapper(UserRepository.class);
        attachmentRepository = sqlSession.getMapper(AttachmentRepository.class);
        System.out.println("BoardService() 생성");
    }

    @Override
    public int write(Post post, Map<String, MultipartFile> files) {
        // 현재 로그인한 작성자 정보
        User user = U.getLoggedUser();
        // 위 정보는 session 의 정보이고, 일단 DB 에서 다시 읽어온다
        user = userRepository.findById(user.getId());
        post.setUser(user); // 글 작성자 세팅

        int cnt = postRepository.save(post); // 글 먼저 저장( 그래야 AI된 pk 값(id) 받아온다.

        addFiles(files, post.getId());

        return cnt;
    }

    @Override
    @Transactional
    public Post detail(Long id) {
        postRepository.incViewCnt(id);
        Post post = postRepository.findById(id);

        if (post != null) {
            List<Attachment> fileList = attachmentRepository.findByPost(post.getId());

            setImage(fileList);
            post.setFileList(fileList);
        }
        return post;
    }

    // 이미지 파일 여부 세팅
    private void setImage(List<Attachment> fileList) {
        for (Attachment attachment : fileList) {

            File f = new File(uploadDir, attachment.getFilename());

            BufferedImage image = null;
            try {
                image = ImageIO.read(f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (image != null) attachment.setImage(true);

        }
    }

    // 페이징 목록
    @Override
    public List<Post> list(Integer page, Model model) {

        // 현재 페이지 parameter
        if (page == null) page = 1;   // 디폴트는 1 page
        if (page < 1) page = 1;

        // 페이징
        // writePages: 한 [페이징] 당 몇개의 페이지가 표시되나
        // pageRows: 한 '페이지'에 몇개의 글을 리스트 할것인가?
        HttpSession session = U.getSession();
        Integer writePages = (Integer) session.getAttribute("writePages");
        if (writePages == null) writePages = WRITE_PAGES;

        Integer pageRows = (Integer) session.getAttribute("pageRows");
        if (pageRows == null) pageRows = PAGE_ROWS;

        session.setAttribute("page", page);  // 현재 페이지 번호 -> session 에 저장

        long cnt = postRepository.countAll();   // 글 목록 전체의 개수
        int totalPage = (int) Math.ceil(cnt / (double) pageRows);  // 총 몇 '페이지' 분량인가?

        // [페이징] 에 표시할 '시작페이지' 와 '마지막페이지'
        int startPage = 0;
        int endPage = 0;

        // 해당 '페이지'의 글 목록
        List<Post> list = null;


        if (cnt > 0) {  // 데이터가 최소 1개 이상 있는 경우만 페이징

            // page 값 보정
            if (page > totalPage) page = totalPage;

            // 몇번째 데이터부터
            int fromRow = (page - 1) * pageRows;

            // [페이징] 에 표시할 '시작페이지' 와 '마지막페이지' 계산
            startPage = (((page - 1) / writePages) * writePages) + 1;
            endPage = startPage + writePages - 1;
            if (endPage >= totalPage) endPage = totalPage;

            // 해당 페이지의 글 목록 읽어오기
            list = postRepository.selectFromRow(fromRow, pageRows);
            model.addAttribute("list", list);
        } else {
            page = 0;
        }
        model.addAttribute("cnt", cnt);  // 전체 글 개수
        model.addAttribute("page", page); // 현재 페이지
        model.addAttribute("totalPage", totalPage);  // 총 '페이지' 수
        model.addAttribute("pageRows", pageRows);  // 한 '페이지' 에 표시할 글 개수

        // [페이징]
        model.addAttribute("url", U.getRequest().getRequestURI());  // 목록 url
        model.addAttribute("writePages", writePages); // [페이징] 에 표시할 숫자 개수
        model.addAttribute("startPage", startPage);  // [페이징] 에 표시할 시작 페이지
        model.addAttribute("endPage", endPage);   // [페이징] 에 표시할 마지막 페이지

        return list;
    }

    @Override
    public Post selectById(Long id) {
        Post post = postRepository.findById(id);

        if (post != null) {
            List<Attachment> fileList = attachmentRepository.findByPost(post.getId());

            setImage(fileList);
            post.setFileList(fileList);
        }

        return post;
    }

    @Override
    public int update(Post post, Map<String, MultipartFile> files, Long[] delfile) {
        int result = 0;

        result = postRepository.update(post);
        // 새로운 첨부파일 추가
        addFiles(files, post.getId());

        // 삭제할 첨부파일들 삭제하기
        if (delfile != null) {
            for (Long fileId : delfile) {
                Attachment file = attachmentRepository.findById(fileId);
                if (file != null) {
                    delFile(file); // 물리적 삭제
                    attachmentRepository.delete(file);
                }
            }
        }

        return result;
    }

    // 특정 첨부파일(id) 를 물리적으로 삭제
    private void delFile(Attachment file) {
        File f = new File(uploadDir, file.getFilename());
        System.out.println("삭제시도 -> " + f.getAbsolutePath());

        if (f.exists()) {
            if (f.delete()) {
                System.out.println("삭제 성공");
            } else {
                System.out.println("삭제 실패");
            }
        } else {
            System.out.println("파일이 존재하지 않습니다.");
        }
    }

    @Override
    public int deleteById(Long id) {
        int result = 0;
        Post post = postRepository.findById(id); // 존재하는 데이터인지 확인
        if (post != null) {
            // 물리적으로 저장된 첨부파일부터 삭제
            List<Attachment> fileList = attachmentRepository.findByPost(id);
            if (fileList != null && !fileList.isEmpty()) {
                for (Attachment file : fileList) {
                    delFile(file);
                }
            }

            // 글삭제 (참조하는 첨부파일, 댓글 등도 같이 삭제 될 것이다 ON DELETE CASCADE)
            result = postRepository.delete(post);
        }
        return result;
    }

    @Override
    public void addFiles(Map<String, MultipartFile> files, Long id) {
        if (files == null) return;

        for (Map.Entry<String, MultipartFile> e : files.entrySet()) {

            // name="upfile##" 인 정보 가져오기 (이유, 다른 웹 에디터에서 업로드 하는 파일과 섞이지 않기 위해...)
            if (!e.getKey().startsWith("upfile")) continue;

            // 첨부파일 정보 출력
            System.out.println("\n첨부파일 정보: " + e.getKey());
            U.printFileInfo(e.getValue()); // MultiFile 정보 출력

            // 물리적인 파일 저장
            Attachment file = upload(e.getValue());

            // 성공하면 DB 에도 저장.
            if (file != null) {
                file.setPost_id(id);
                attachmentRepository.save(file);
            }

        }
    }

    // 물리적으로 서버에 파일 저장. 중복된 파일 이름 -> rename 처리
    private Attachment upload(MultipartFile multipartFile) {
        Attachment attachment = null;

        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) return null;

        // 원본 파일명
        String sourceName = StringUtils.cleanPath(originalFilename);

        // 저장될 파일명
        String fileNmae = sourceName;

        // 파일명이 중복되는지 확인
        File file = new File(uploadDir, fileNmae);
        if (file.exists()) { // 이미 존재하는 파일명, 중복된다면 다름 이름으로 rename 하여 저장.
            // a.txt => a_2378142783946.txt  : time stamp 값을 활용할거다!
            // "a" => "a_2378142783946"  : 확장자 없는 경우
            int pos = fileNmae.lastIndexOf(".");
            if (pos > -1) { // 확장자가 있는 경
                String name = fileNmae.substring(0, pos); // 파일 '이름'
                String ext = fileNmae.substring(pos); // 파일 '확장자'

                // 타임스탬프 활용
                fileNmae = name + "_" + System.currentTimeMillis() + ext;
            } else { // 확장자가 없는 경우
                fileNmae += "_" + System.currentTimeMillis();
            }

        }
        // 저장할 파일명
        System.out.println("fileName: " + fileNmae);

        // java.nio.*

        Path copyLocation = Paths.get(new File(uploadDir, fileNmae).getAbsolutePath());
        System.out.println(copyLocation);

        try { // 물리적으로 저장
            Files.copy(
                    multipartFile.getInputStream(),
                    copyLocation,
                    StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        attachment = Attachment.builder()
                .filename(fileNmae)
                .sourcename(sourceName)
                .build();
        System.out.println("upload 성공");
        return attachment;
    } // end upload
}
