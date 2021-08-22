obj = VideoReader('output_point.avi');
number_of_frames = obj.NumberOfFrames;
framerate = obj.FrameRate;

obj = vision.VideoFileReader('output_point.avi');
outputVideo = vision.VideoFileWriter('trimmed_point.avi','FrameRate',framerate);

%for columns
for i = 1:number_of_frames
    img = step(obj);
    newimg = img(89:632,161:1120,:);
    step(outputVideo,newimg);
end
release(outputVideo);
