import React from 'react';
import {shallow,mount} from 'enzyme';
import NameComponent from '../../../app/multistep/projectcreate/NameComponent.jsx';
import sinon from 'sinon';

describe("NameComponent", ()=>{
    it("should calculate a temporary filename by replacing all non-alphanumeric characters by underscores", ()=>{
        const updatedCb = sinon.spy();
        const rendered = shallow(<NameComponent projectName="My prøject with funny characters!!!"
                                                fileName="initialfile"
                                                selectionUpdated={updatedCb}/>);

        const result = rendered.instance().makeAutoFilename("My prøject with funny characters!!!");
        expect(result).toEqual("my_pr_ject_with_funny_characters___");
    });

    it("should call selectionUpdated when the user types into the project name box", ()=>{
        const updatedCb = sinon.spy();
        const rendered = shallow(<NameComponent projectName="My prøject with funny characters!!!"
                                                fileName="initialfile"
                                                selectionUpdated={updatedCb}/>);

        const projectNameInput = rendered.find('#projectNameInput');
        projectNameInput.simulate("change",{target: {value: "new project"}});
        expect(updatedCb.calledWith({projectName: "new project", fileName: "new_project", autoNameFile: true})).toBeTruthy();
    });

    it("should call selectionUpdated when the user types into the file name box", ()=>{
        const updatedCb = sinon.spy();
        const rendered = shallow(<NameComponent projectName="My project name"
                                                fileName="initialfile"
                                                selectionUpdated={updatedCb}/>);

        const fileNameInput = rendered.find('#fileNameInput');
        fileNameInput.simulate("change",{target: {value: "new file"}});
        expect(updatedCb.calledWith({projectName: "My project name", fileName: "new file", autoNameFile: true})).toBeTruthy();
    });
});