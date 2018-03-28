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
        expect(result).toMatch(/\d{8}_my_pr_ject_with_funny_characters/);   //filename part is limited to 32 chars
    });

    it("should call selectionUpdated when the user types into the project name box", ()=>{
        const updatedCb = sinon.spy();
        const rendered = shallow(<NameComponent projectName="My prøject with funny characters!!!"
                                                fileName="initialfile"
                                                selectionUpdated={updatedCb}/>);

        const projectNameInput = rendered.find('#projectNameInput');
        projectNameInput.simulate("change",{target: {value: "new project"}});
        expect(updatedCb.calledOnce).toBeTruthy();
        expect(updatedCb.args[0][0]['projectName']).toEqual("new project");
        expect(updatedCb.args[0][0]['autoNameFile']).toBeTruthy();
        expect(updatedCb.args[0][0]['fileName']).toMatch(/\d{8}_new_project/);
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