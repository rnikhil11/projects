clear;
filename = 'input1.mp4';

hVideoSrc = VideoReader(filename);
framerate = hVideoSrc.FrameRate;

hVideoSrc = vision.VideoFileReader(filename, 'ImageColorSpace', 'Intensity');
outputVideo = vision.VideoFileWriter('output_surf.avi','FrameRate',framerate);

% Reset the video source to the beginning of the file.
reset(hVideoSrc);

hVPlayer = vision.VideoPlayer; % Create video viewer

% Process all frames in the video
movMean = step(hVideoSrc);
imgB = movMean;
imgBp = imgB;
correctedMean = imgBp;
ii = 2;
Hcumulative = eye(3);
first = true;

while ~isDone(hVideoSrc)
    % Read in new frame
    imgA = imgB; % z^-1
    imgAp = imgBp; % z^-1
    
    if(first)
        first = false;
        step(outputVideo,imgAp);
    end
    frame1 = imgAp;    
    imgB = step(hVideoSrc);
    movMean = movMean + imgB;
    
    % Estimate transform from frame A to frame B, and fit as an s-R-t
%     pointsA = detectSURFFeatures(imgA);
%     [featuresA, pointsA] = extractFeatures(imgA, pointsA);

    pointsA = detectSURFFeatures(imgA);
    [featuresA, pointsA] = extractFeatures(imgA, pointsA);

    pointsB = detectSURFFeatures(imgB);
    [featuresB, pointsB] = extractFeatures(imgB, pointsB);
    
    indexPairs = matchFeatures(featuresA, featuresB);
    pointsA = pointsA(indexPairs(:, 1), :);
    pointsB = pointsB(indexPairs(:, 2), :);
    
    [HsRt, pointsBm, pointsAm] = estimateGeometricTransform(pointsB, pointsA, 'affine');
    
%     H = cvexEstStabilizationTform(imgA,imgB);
%     HsRt = cvexTformToSRT(H);
    
    Hcumulative = HsRt.T * Hcumulative;
    imgBp = imwarp(imgB,affine2d(Hcumulative),'OutputView',imref2d(size(imgB)));
    
    frame2 = imgBp;
            
    zros = find(frame2 == 0);
    
    %frame1 = imwarp(imgA,affine2d(Hcumulative),'OutputView',imref2d(size(imgA)));
    
    frame2(zros) = frame1(zros);
    
    imgBp = frame2;
    
    step(outputVideo,frame2);
    correctedMean = correctedMean + imgBp;
    
    ii = ii+1;
end

%step(outputVideo,imgBp);
correctedMean = correctedMean/(ii-2);
movMean = movMean/(ii-2);

% Here you call the release method on the objects to close any open files
% and release memory.
release(outputVideo);
release(hVideoSrc);
release(hVPlayer);