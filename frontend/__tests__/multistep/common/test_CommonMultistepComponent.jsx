import React from 'react';
import {shallow,mount} from 'enzyme';
import moxios from 'moxios';
import CommonMultistepComponent from '../../../app/multistep/common/CommonMultistepComponent.jsx';
import sinon from 'sinon';
import assert from 'assert';

class DummyComponent extends CommonMultistepComponent {
    constructor(props){
        super(props);
        this.state = {
            key: 'value'
        }
    }

    render(){
        return <p>{this.state.key}</p>
    }
}

describe("CommonMultistepComponent", ()=>{
    it("should set a callback method for ComponentWillUpdate", ()=>{
        const spy = sinon.spy();
        const rendered = mount(<DummyComponent valueWasSet={spy}/>);
        expect(rendered.find('p').text()).toEqual('value');

        rendered.instance().setState({key: 'second value'});
        assert(spy.calledOnce);
        rendered.update();
        expect(rendered.find('p').text()).toEqual("second value");

    });
});