import React from 'react';
import {shallow,mount} from 'enzyme';
import TypeSelectorComponent from '../../../app/multistep/projecttemplate/TypeSelectorComponent.jsx';

describe("TypeSelectorComponent", ()=>{
    it("should display a list from its props", ()=>{
        const fakeProjectTypes = [
            {
                id: 1,
                name: "project type one"
            },
            {
                id: 6,
                name: "project type two"
            }
        ];

        const rendered = shallow(<TypeSelectorComponent projectTypes={fakeProjectTypes}/>);

        const selector = rendered.find('#project_type_selector');
        expect(selector.childAt(0).props().value).toEqual(1);
        expect(selector.childAt(0).text()).toEqual("project type one");
        expect(selector.childAt(1).props().value).toEqual(6);
        expect(selector.childAt(1).text()).toEqual("project type two");
    });
});